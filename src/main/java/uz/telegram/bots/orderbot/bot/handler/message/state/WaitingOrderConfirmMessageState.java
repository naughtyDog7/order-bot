package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.jowi.JowiService;
import uz.telegram.bots.orderbot.bot.service.OrderService;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    private final BadRequestHandler badRequestHandler;

    private static final ZoneId tashkentZoneId = ZoneId.of("GMT+5");

    @Autowired
    WaitingOrderConfirmMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                                    OrderService orderService, KeyboardFactory kf, LockFactory lf, JowiService jowiService, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.kf = kf;
        this.lf = lf;
        this.jowiService = jowiService;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }
        String messageText = message.getText();
        handleMessageText(bot, telegramUser, rb, messageText);
    }

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String messageText) {
//        String btnCancelOrder = rb.getString("btn-cancel-order");
        String btnCheckStatus = rb.getString("btn-check-order-status");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.findActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (messageText.equals(btnCheckStatus)) {
                handleCheckStatus(bot, telegramUser, rb, order);
            } /*else if (text.equals(btnCancelOrder)) {
                handleCancelOrder(bot, telegramUser, rb, order);
            }*/ else {
                badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleCheckStatus(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        SendMessage waitingForServerResponseMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("waiting-for-order-confirm-message"));
        try {
            bot.execute(waitingForServerResponseMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCancelOrder(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        try {
            jowiService.cancelOrderOnServer(order, "Пользователь отменил заказ");
            orderService.deleteOrder(order);
            sendCancellationMessage(bot, telegramUser, rb.getString("order-was-cancelled-by-user"));
            handleToMainMenu(bot, telegramUser, rb);
        } catch (IOException e) {
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendCancellationMessage(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                         String newMessageText) throws TelegramApiException {
        SendMessage cancellationMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(newMessageText);
        bot.execute(cancellationMessage);
    }

    private void handleToMainMenu(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        ToMainMenuHandler.builder()
                .rb(rb)
                .telegramUser(telegramUser)
                .bot(bot)
                .service(userService)
                .kf(kf)
                .build()
                .handleToMainMenu();
    }
}
