package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.JowiService;
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
    private final JowiService jowiService;

    @Autowired
    WaitingOrderConfirmMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                                    OrderService orderService, KeyboardFactory kf, LockFactory lf, JowiService jowiService) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.kf = kf;
        this.lf = lf;
        this.jowiService = jowiService;
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
        String btnCancelOrder = rb.getString("btn-cancel-order");
        String btnCheckStatus = rb.getString("btn-check-order-status");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.getActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (text.equals(btnCheckStatus)) {
                handleCheckStatus(bot, telegramUser, rb, order);
            } else if (text.equals(btnCancelOrder)) {
                handleCancelOrder(bot, telegramUser, rb, order);
            } else {
                DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleCancelOrder(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        try {
            jowiService.cancelOrderOnServer(order, "Пользователь отменил заказ");
            orderService.deleteOrder(order);
            SendMessage sendMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("order-was-cancelled-by-user"));
            bot.execute(sendMessage);
            ToMainMenuHandler.builder()
                    .rb(rb)
                    .telegramUser(telegramUser)
                    .bot(bot)
                    .service(userService)
                    .kf(kf)
                    .build()
                    .handleToMainMenu();
        } catch (IOException e) {
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
            jowiService.cancelOrderOnServer(order, "Достигнут максимум ожидания");
            orderService.deleteOrder(order);
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
}