package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.CategoryService;
import uz.telegram.bots.orderbot.bot.service.OrderService;
import uz.telegram.bots.orderbot.bot.service.ProductWithCountService;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.handler.message.state.UserState.SETTINGS;

@Component
@Slf4j
class SettingsPhoneNumState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final ProductWithCountService pwcService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    SettingsPhoneNumState(ResourceBundleFactory rbf, TelegramUserService userService,
                          OrderService orderService, CategoryService categoryService,
                          ProductWithCountService pwcService, KeyboardFactory kf, KeyboardUtil ku,
                          LockFactory lf, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.pwcService = pwcService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as contact, back button, confirm button, and change button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        if (message.hasText()) {
            handleMessageText(bot, telegramUser, message.getText(), rb);
        } else if (message.hasContact()) {
            handleContact(bot, telegramUser, message, rb);
        } else {
            badRequestHandler.handleContactBadRequest(bot, telegramUser, rb);
        }
    }

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                   String messageText, ResourceBundle rb) {
        String btnBack = rb.getString("btn-back");
        if (messageText.equals(btnBack))
            handleBack(bot, telegramUser, rb);
        else
            handlePhoneNum(bot, telegramUser, rb, messageText);
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage settingsMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("configure-settings"));
        ku.setSettingsKeyboard(settingsMessage, rb, telegramUser.getLangISO(), kf, telegramUser.getPhoneNum() != null);

        try {
            bot.execute(settingsMessage);
            telegramUser.setCurState(SETTINGS);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handlePhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                ResourceBundle rb, String phoneNum) {
        try {
            userService.checkAndSetPhoneNum(telegramUser, phoneNum);
            userService.save(telegramUser);
            log.info("Phone num updated, telegram user " + telegramUser);
            handlePhoneNumUpdated(bot, telegramUser, rb);
        } catch (IllegalArgumentException e) {
            badRequestHandler.handleBadPhoneNumber(bot, telegramUser, rb);
        }
    }

    private void handlePhoneNumUpdated(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        try {
            sendPhoneNumUpdatedMessage(bot, telegramUser, rb);
            telegramUser.setCurState(SETTINGS);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoneNumUpdatedMessage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) throws TelegramApiException {
        SendMessage phoneNumSavedMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("phone-set-success"));
        ku.setSettingsKeyboard(phoneNumSavedMessage, rb, telegramUser.getLangISO(), kf, true);
        bot.execute(phoneNumSavedMessage);
    }

    private void handleContact(TelegramLongPollingBot bot, TelegramUser telegramUser, Message message, ResourceBundle rb) {
        Contact contact = message.getContact();
        String phoneNum = contact.getPhoneNumber();
        handlePhoneNum(bot, telegramUser, rb, phoneNum);
    }
}
