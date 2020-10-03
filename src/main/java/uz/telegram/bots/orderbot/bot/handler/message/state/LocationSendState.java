package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Location;
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

import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.FINAL_CONFIRMATION_KEYBOARD;

@Component
class LocationSendState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final PaymentInfoService paymentInfoService;
    private final CategoryService categoryService;
    private final ProductWithCountService pwcService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final TextUtil tu;
    private final LockFactory lf;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    LocationSendState(ResourceBundleFactory rbf, TelegramUserService userService,
                      OrderService orderService, PaymentInfoService paymentInfoService,
                      CategoryService categoryService, ProductWithCountService pwcService,
                      KeyboardFactory kf, KeyboardUtil ku, TextUtil tu, LockFactory lf, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.paymentInfoService = paymentInfoService;
        this.categoryService = categoryService;
        this.pwcService = pwcService;
        this.kf = kf;
        this.ku = ku;
        this.tu = tu;
        this.lf = lf;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as location, back button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.findActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (message.hasText()) {
                String text = message.getText();
                String btnBack = rb.getString("btn-back");
                if (text.equals(btnBack))
                    handleBack(bot, telegramUser, rb, order);
                else
                    badRequestHandler.handleLocationBadRequest(bot, telegramUser, rb);
            } else if (message.hasLocation()) {
                Location location = message.getLocation();
                handleLocation(bot, telegramUser, rb, order, location);
            } else {
                badRequestHandler.handleLocationBadRequest(bot, telegramUser, rb);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleLocation(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, Location location) {
        TelegramLocation tLocation = TelegramLocation.of(location.getLatitude(), location.getLongitude());
        PaymentInfo paymentInfo = paymentInfoService.findByOrderId(order.getId());
        paymentInfo.setOrderLocation(tLocation);
        paymentInfo.setPaymentMethod(PaymentInfo.PaymentMethod.CASH); //Set method to cash because no other available for now
        paymentInfoService.save(paymentInfo);

        StringBuilder messageText = new StringBuilder(rb.getString("confirm-before-send-to-server")).append("\n");
        List<ProductWithCount> products = pwcService.findByOrderId(order.getId());
        tu.appendProducts(messageText, products, rb, true, order.getDeliveryPrice());
        tu.appendPhoneNum(messageText, telegramUser.getPhoneNum(), rb);
        tu.appendNoNameLocation(messageText, rb);
        tu.appendPaymentMethod(messageText, paymentInfo.getPaymentMethod(), rb);

        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(messageText.toString());
        setConfirmOrderKeyboard(sendMessage, telegramUser.getLangISO());
        try {
            bot.execute(sendMessage);
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

    //Temp disabling payment method choosing
    /*private void handleLocation(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, Location location) {
        TelegramLocation tLocation = TelegramLocation.of(location.getLatitude(), location.getLongitude());
        PaymentInfo paymentInfo = paymentInfoService.findByOrderId(order.getId());
        paymentInfo.setOrderLocation(tLocation);
        paymentInfoService.save(paymentInfo);

        StringBuilder curOrderText = new StringBuilder(rb.getString("your-order")).append("\n");
        List<ProductWithCount> products = pwcService.findByOrderId(order.getId());
        tu.appendProducts(curOrderText, products, rb, true, order.getDeliveryPrice());
        tu.appendPhoneNum(curOrderText, telegramUser.getPhoneNum(), rb);
        tu.appendNoNameLocation(curOrderText, rb);
        SendMessage sendMessage1 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(curOrderText.toString());
        SendMessage sendMessage2 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("choose-payment-method"));
        setPaymentMethodKeyboard(sendMessage1, telegramUser.getLangISO());
        try {
            bot.execute(sendMessage1);
            bot.execute(sendMessage2);
            telegramUser.setCurState(TelegramUser.UserState.PAYMENT_METHOD_CHOOSE);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setPaymentMethodKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(PAYMENT_METHOD_CHOOSE, langISO);
        sendMessage.setReplyMarkup(ku.addBackButtonLast(keyboard, langISO)
                .setResizeKeyboard(true));
    }*/

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        List<Category> categories = categoryService.findNonEmptyByOrderId(order.getId());
        int basketNumItems = pwcService.getBasketItemsCount(order.getId());
        ToOrderMainHandler.builder()
                .bot(bot)
                .service(userService)
                .rb(rb)
                .telegramUser(telegramUser)
                .ku(ku)
                .kf(kf)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems, false);
    }
}
