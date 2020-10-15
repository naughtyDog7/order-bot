package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.handler.message.state.UserState.LANGUAGE_CONFIGURE;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.LANG_KEYBOARD;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.PHONE_NUM_ENTER_KEYBOARD;

@Component
class SettingsMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    SettingsMessageState(ResourceBundleFactory rbf, TelegramUserService service, KeyboardFactory kf, KeyboardUtil ku, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
        this.ku = ku;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as change lang, save num, change num, and back button
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
        String changeLanguage = rb.getString("btn-settings-language-choose");
        String phoneNumSet = rb.getString("btn-set-new-phone-num");
        String phoneNumUpdate = rb.getString("btn-change-existing-phone-num");
        String back = rb.getString("btn-back");
        if (messageText.equals(changeLanguage)) {
            handleChangeLanguage(bot, telegramUser, rb);
        } else if (messageText.equals(phoneNumSet) || messageText.equals(phoneNumUpdate)) {
            handlePhoneNum(bot, telegramUser, rb);
        } else if (messageText.equals(back)) {
            handleBack(bot, telegramUser, rb);
        } else {
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
        }
    }

    private void handleChangeLanguage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage langChooseMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("language-choose"));
        setLangKeyboard(langChooseMessage);
        try {
            bot.execute(langChooseMessage);
            telegramUser.setCurState(LANGUAGE_CONFIGURE);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setLangKeyboard(SendMessage sendMessage) {
        sendMessage.setReplyMarkup(kf.getKeyboard(LANG_KEYBOARD, "") //lang iso empty because currently we have no lang, get default bundle
                .setResizeKeyboard(true));
    }

    private void handlePhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        try {
            sendCurrentPhoneNumMessage(bot, telegramUser, rb);
            sendPhoneNumRequestMessage(bot, telegramUser, rb);
            telegramUser.setCurState(UserState.SETTINGS_PHONE_NUM);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendCurrentPhoneNumMessage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) throws TelegramApiException {
        String phoneNum = telegramUser.getPhoneNum();
        if (phoneNum != null) {
            bot.execute(new SendMessage()
                            .setChatId(telegramUser.getChatId())
                            .setText(rb.getString("your-current-phone-num-is") + " " + phoneNum));
        }
    }

    private void sendPhoneNumRequestMessage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) throws TelegramApiException {
        SendMessage phoneNumRequestMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("press-to-send-contact"));
        setPhoneKeyboard(phoneNumRequestMessage, telegramUser.getLangISO());
        bot.execute(phoneNumRequestMessage);
    }

    private void setPhoneKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(PHONE_NUM_ENTER_KEYBOARD, langISO);
        sendMessage.setReplyMarkup(
                ku.addBackButtonLast(keyboard, langISO)
                        .setResizeKeyboard(true));
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        ToMainMenuHandler.builder()
                .bot(bot)
                .rb(rb)
                .telegramUser(telegramUser)
                .service(service)
                .kf(kf)
                .build()
                .handleToMainMenu();
    }
}
