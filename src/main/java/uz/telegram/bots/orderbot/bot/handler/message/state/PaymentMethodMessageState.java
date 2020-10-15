package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.jetbrains.annotations.NotNull;
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
import java.util.Optional;
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
    private final BadRequestHandler badRequestHandler;

    @Autowired
    PaymentMethodMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                              CategoryService categoryService, OrderService orderService,
                              ProductWithCountService pwcService, PaymentInfoService paymentInfoService,
                              KeyboardFactory kf, KeyboardUtil ku, TextUtil tu, LockFactory lf, BadRequestHandler badRequestHandler) {
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
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as payment method or button back
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText())
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
        else
            handleMessageText(bot, telegramUser, rb, message.getText());

    }

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                   ResourceBundle rb, String messageText) {
        String btnBack = rb.getString("btn-back");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.findActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (messageText.equals(btnBack)) {
                handleBack(bot, telegramUser, rb, order);
            } else {
                handlePotentialPaymentMethod(bot, telegramUser, rb, messageText, order);
            }

        } finally {
            lock.unlock();
        }
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
                .handleToOrderMain(basketItemsCount, ToOrderMainHandler.CallerPlace.OTHER);
    }

    private void handlePotentialPaymentMethod(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                              ResourceBundle rb, String messageText, Order order) {
        PaymentInfo paymentInfo = paymentInfoService.findByOrderId(order.getId());
        Optional<String> optChosenMethod =
                getChosenPaymentMethodFromString(rb,
                        messageText, paymentInfo);
        if (optChosenMethod.isEmpty()) {
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
        } else {
            handlePaymentMethod(bot, telegramUser, rb, order, paymentInfo, optChosenMethod.get());
        }
    }

    private Optional<String> getChosenPaymentMethodFromString(ResourceBundle rb, String text,
                                                              PaymentInfo paymentInfo) {
        String chosenMethod = null;
        String btnCash = rb.getString("btn-cash");
        String btnClick = rb.getString("btn-click");
        String btnPayme = rb.getString("btn-payme");
        if (text.equals(btnCash)) {
            paymentInfo.setPaymentMethod(CASH);
            chosenMethod = btnCash;
        } else if (text.equals(btnClick)) {
            paymentInfo.setPaymentMethod(CLICK);
            chosenMethod = btnClick;
        } else if (text.equals(btnPayme)) {
            paymentInfo.setPaymentMethod(PAYME);
            chosenMethod = btnPayme;
        }
        return Optional.ofNullable(chosenMethod);
    }

    private void handlePaymentMethod(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                     ResourceBundle rb, Order order, PaymentInfo paymentInfo, String chosenMethod) {
        paymentInfo = paymentInfoService.save(paymentInfo);

        try {
            sendPaymentMethodChosenMessage(bot, telegramUser, rb, chosenMethod);
            sendFinalConfirmationMessage(bot, telegramUser, rb, order, paymentInfo);
            telegramUser.setCurState(UserState.FINAL_CONFIRMATION);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPaymentMethodChosenMessage(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                                ResourceBundle rb, String chosenMethod) throws TelegramApiException {
        SendMessage paymentMethodChosenMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("payment-method-chosen") + ": \"" + chosenMethod + "\"");
        bot.execute(paymentMethodChosenMessage);
    }


    private void sendFinalConfirmationMessage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, PaymentInfo paymentInfo) throws TelegramApiException {
        StringBuilder finalConfirmationMessageText = prepareFinalConfirmationMessageText(
                telegramUser, rb, order, paymentInfo);

        SendMessage finalConfirmationMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(finalConfirmationMessageText.toString());
        setConfirmOrderKeyboard(finalConfirmationMessage, telegramUser.getLangISO());
        bot.execute(finalConfirmationMessage);
    }

    @NotNull
    private StringBuilder prepareFinalConfirmationMessageText(TelegramUser telegramUser, ResourceBundle rb,
                                                              Order order, PaymentInfo paymentInfo) {
        StringBuilder messageText = new StringBuilder(rb.getString("confirm-before-send-to-server")).append("\n");
        List<ProductWithCount> products = pwcService.findByOrderId(order.getId());
        tu.appendProducts(messageText, products, rb, true, order.getDeliveryPrice());
        tu.appendPhoneNum(messageText, telegramUser.getPhoneNum(), rb);
        tu.appendNoNameLocation(messageText, rb);
        tu.appendPaymentMethod(messageText, paymentInfo.getPaymentMethod(), rb);
        return messageText;
    }

    private void setConfirmOrderKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(FINAL_CONFIRMATION_KEYBOARD, langISO);
        keyboard = ku.addBackButtonLast(keyboard, langISO);
        sendMessage.setReplyMarkup(ku.concatLastTwoRows(keyboard)
                .setResizeKeyboard(true));
    }
}
