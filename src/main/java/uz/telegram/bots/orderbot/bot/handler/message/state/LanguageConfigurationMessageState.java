package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.handler.message.state.UserState.SETTINGS;

@Component
class LanguageConfigurationMessageState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    LanguageConfigurationMessageState(ResourceBundleFactory rbf, TelegramUserService service, KeyboardFactory kf,
                                      KeyboardUtil ku, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
        this.ku = ku;
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

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                   ResourceBundle rb, String messageText) {
        if (messageText.equals(rb.getString("btn-uzb-lang")))
            telegramUser.setLangISO("uzb");
        else if (messageText.equals(rb.getString("btn-rus-lang")))
            telegramUser.setLangISO("rus");
        else {
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }
        handleLangConfigured(bot, telegramUser);
    }

    private void handleLangConfigured(TelegramLongPollingBot bot, TelegramUser telegramUser) {
        ResourceBundle rb;
        String newLangISO = telegramUser.getLangISO();
        rb = rbf.getMessagesBundle(newLangISO);
        try {
            sendSettingsMessage(bot, telegramUser, rb, newLangISO);
            telegramUser.setCurState(SETTINGS);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendSettingsMessage(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                     ResourceBundle rb, String newLangISO) throws TelegramApiException {
        SendMessage langChosenSuccessfullyMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("language-chosen"));
        ku.setSettingsKeyboard(langChosenSuccessfullyMessage, rb, newLangISO, kf, telegramUser.getPhoneNum() != null);
        bot.execute(langChosenSuccessfullyMessage);
    }
}
