package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.OrderService;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
class WaitingOrderConfirmMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final KeyboardFactory kf;
    private final LockFactory lf;

    @Autowired
    WaitingOrderConfirmMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                                    OrderService orderService, KeyboardFactory kf, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.kf = kf;
        this.lf = lf;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }
        String text = message.getText();
        String btnCheckStatus = rb.getString("btn-check-order-status");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            if (text.equals(btnCheckStatus)) {
                Order order = orderService.getActive(telegramUser)
                        .orElseThrow(() -> new AssertionError("Order must be present at this point"));
                handleCheckStatus(bot, telegramUser, rb, order);
            }
        } finally {
            lock.unlock();
        }
    }

    private static final ZoneId tashkentZoneId = ZoneId.of("GMT+5");

    private void handleCheckStatus(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        if (LocalDateTime.now(tashkentZoneId).minusMinutes(5).isAfter(order.getRequestSendTime())) {
            // if passed more than 5 minutes but still at this state, no one accepted order, so cancel it
            handleTimeIsUp(bot, telegramUser, rb, order);
        } else {
            SendMessage sendMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("waiting-for-order-confirm-message"));
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleTimeIsUp(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        try {
            orderService.cancelOrderOnServer(order, "Достигнут максимум ожидания");
            orderService.cancelOrder(order);
            SendMessage sendMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("order-confirm-time-is-up"));
            bot.execute(sendMessage);
            ToMainMenuHandler.builder()
                    .bot(bot)
                    .telegramUser(telegramUser)
                    .service(userService)
                    .rb(rb)
                    .kf(kf)
                    .build()
                    .handleToMainMenu();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
            throw new UncheckedIOException(e);
        }
    }

    /*
//      0 	Новый заказ
//      1 	Заказ принят в работу
//      2 	Заказ отменен
//      3 	Заказ отправлен клиенту
//      4 	Заказ доставлен
        try {
            int status = orderService.getOrderStatusValueFromServer(order.getOrderId());
            switch (status) {
                case 0:
                    handleNewCheck(bot, telegramUser, rb, order);
                    break;
                case 1:
                    handleAcceptedCheck(bot, telegramUser, rb, order);
                    break;
                case 2:
                    throw new AssertionError("Should not check cancelled ever, because after cancel check status should not be reached");
                case 3:
                    handleSentCheck(bot, telegramUser, rb, order);
                    break;
                case 4:
                    throw new AssertionError("Should not check already delivered ever, because after deliver check status should not be reached");
                default:
                    throw new IOException("Waiting for statuses 0-4, received: " + status);
            }
        } catch (IOException e) {
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
            throw new UncheckedIOException(e);
        }
*/
}