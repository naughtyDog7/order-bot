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

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.SETTINGS;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.SETTINGS_KEYBOARD;

@Component
class LanguageConfigurationMessageState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;

    @Autowired
    LanguageConfigurationMessageState(ResourceBundleFactory rbf, TelegramUserService service, KeyboardFactory kf, KeyboardUtil ku) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
        this.ku = ku;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        if (!message.hasText())
            return;
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        String lang = message.getText();
        if (lang.contains(rb.getString("btn-uzb-lang")))
            telegramUser.setLangISO("uzb");
        else if (lang.contains(rb.getString("btn-rus-lang")))
            telegramUser.setLangISO("rus");

        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("language-chosen"));

        setSettingsKeyboard(sendMessage, telegramUser.getLangISO());
        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(SETTINGS);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setSettingsKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(SETTINGS_KEYBOARD, langISO)
                .setResizeKeyboard(true);
        keyboard = ku.addBackButtonLast(keyboard, langISO); //if not other buttons, user KeyboardFactory#getBackButtonKeyboard
        sendMessage.setReplyMarkup(keyboard
                .setResizeKeyboard(true));
    }
}
