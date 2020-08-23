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
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.LANGUAGE_CONFIGURE;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.LANG_KEYBOARD;

@Component
class SettingsMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;

    @Autowired
    SettingsMessageState(ResourceBundleFactory rbf, TelegramUserService service, KeyboardFactory kf) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        Message message = update.getMessage();
        if (!message.hasText())
            return;

        String changeLanguage = rb.getString("btn-settings-language-choose");
        String back = rb.getString("btn-back");

        String messageText = message.getText();

        if (messageText.equals(changeLanguage)) {
            handleChangeLanguage(bot, telegramUser, rb);
        } else if (messageText.equals(back)) {
            handleBack(bot, telegramUser, rb);
        }
    }

    private void handleChangeLanguage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("language-choose"));
        setLangKeyboard(sendMessage);
        try {
            bot.execute(sendMessage);
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
