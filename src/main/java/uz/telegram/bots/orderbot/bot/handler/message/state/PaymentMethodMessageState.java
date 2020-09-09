package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.*;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

import static uz.telegram.bots.orderbot.bot.user.PaymentInfo.PaymentMethod.*;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.FINAL_CONFIRMATION_KEYBOARD;

@Component
class PaymentMethodMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final ProductWithCountService pwcService;
    private final PaymentInfoService paymentInfoService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final TextUtil tu;
    private final LockFactory lf;

    @Autowired
    PaymentMethodMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                              CategoryService categoryService, OrderService orderService,
                              ProductWithCountService pwcService, PaymentInfoService paymentInfoService,
                              KeyboardFactory kf, KeyboardUtil ku, TextUtil tu, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.pwcService = pwcService;
        this.paymentInfoService = paymentInfoService;
        this.kf = kf;
        this.ku = ku;
        this.tu = tu;
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
        String btnBack = rb.getString("btn-back");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.findActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (text.equals(btnBack)) {
                handleBack(bot, telegramUser, rb, order);
            } else {
                handlePaymentMethod(bot, telegramUser, rb, text, order);
            }

        } finally {
            lock.unlock();
        }
    }

    private void handlePaymentMethod(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String text, Order order) {
        String btnCash = rb.getString("btn-cash");
        String btnClick = rb.getString("btn-click");
        String btnPayme = rb.getString("btn-payme");
        PaymentInfo paymentInfo = paymentInfoService.findByOrderId(order.getId());

        String chosenMethod;
        if (text.equals(btnCash)) {
            paymentInfo.setPaymentMethod(CASH);
            chosenMethod = btnCash;
        } else if (text.equals(btnClick)) {
            paymentInfo.setPaymentMethod(CLICK);
            chosenMethod = btnClick;
        } else if (text.equals(btnPayme)) {
            paymentInfo.setPaymentMethod(PAYME);
            chosenMethod = btnPayme;
        } else {
            DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }
        paymentInfo = paymentInfoService.save(paymentInfo);
        SendMessage sendMessage1 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("payment-method-chosen") + ": \"" + chosenMethod + "\"");

        StringBuilder messageText = new StringBuilder(rb.getString("confirm-before-send-to-server")).append("\n");
        List<ProductWithCount> products = pwcService.findByOrderId(order.getId());
        tu.appendProducts(messageText, products, rb, true, order.getDeliveryPrice());
        tu.appendPhoneNum(messageText, telegramUser.getPhoneNum(), rb);
        tu.appendNoNameLocation(messageText, rb);
        tu.appendPaymentMethod(messageText, paymentInfo.getPaymentMethod(), rb);

        SendMessage sendMessage2 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(messageText.toString());

        setConfirmOrderKeyboard(sendMessage2, telegramUser.getLangISO());
        try {
            bot.execute(sendMessage1);
            bot.execute(sendMessage2);
            telegramUser.setCurState(TelegramUser.UserState.FINAL_CONFIRMATION);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setConfirmOrderKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(FINAL_CONFIRMATION_KEYBOARD, langISO);
        keyboard = ku.addBackButtonLast(keyboard, langISO);
        sendMessage.setReplyMarkup(ku.concatLastTwoRows(keyboard)
                .setResizeKeyboard(true));
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        List<Category> categories = categoryService.findNonEmptyByOrderId(order.getId());
        int basketItemsCount = pwcService.getBasketItemsCount(order.getId());
        ToOrderMainHandler.builder()
                .bot(bot)
                .telegramUser(telegramUser)
                .service(userService)
                .kf(kf)
                .ku(ku)
                .rb(rb)
                .categories(categories)
                .build()
                .handleToOrderMain(basketItemsCount, false);
    }
}
