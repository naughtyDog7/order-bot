package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@Component
class FinalConfirmationMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final ProductWithCountService pwcService;
    private final PaymentInfoService paymentInfoService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;

    @Autowired
    FinalConfirmationMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                                  CategoryService categoryService, OrderService orderService,
                                  ProductWithCountService pwcService, PaymentInfoService paymentInfoService,
                                  KeyboardFactory kf, KeyboardUtil ku, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.pwcService = pwcService;
        this.paymentInfoService = paymentInfoService;
        this.kf = kf;
        this.ku = ku;
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
        String btnConfirm = rb.getString("btn-confirm");
        String btnBack = rb.getString("btn-back");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.getActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (text.equals(btnBack)) {
                handleBack(bot, telegramUser, rb, order);
            } else if (text.equals(btnConfirm)){
                handleConfirm(bot, telegramUser, rb, order);
            } else {
                DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleConfirm(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        try {
            orderService.postOrder(order, telegramUser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        List<Category> categories = categoryService.findNonEmptyByOrderId(order.getId());
        int basketNumItems = pwcService.getBasketItemsCount(order.getId());
        ToOrderMainHandler.builder()
                .bot(bot)
                .service(userService)
                .telegramUser(telegramUser)
                .ku(ku)
                .kf(kf)
                .rb(rb)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems);
    }
}
