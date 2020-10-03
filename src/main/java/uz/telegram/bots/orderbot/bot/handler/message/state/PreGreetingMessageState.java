package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.LANG_KEYBOARD;

@Component
@Slf4j
class PreGreetingMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;

    @Autowired
    PreGreetingMessageState(ResourceBundleFactory rbf, TelegramUserService service, KeyboardFactory kf) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
    }

    @Override
    //input ignored, but usually is '/start'
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        log.info("New user registered: " + telegramUser);
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        SendMessage sendMessage1 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("greeting"));
        SendMessage sendMessage2 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("language-choose"));
        setLangKeyboard(sendMessage2);
        try {
            bot.execute(sendMessage1);
            bot.execute(sendMessage2);
            telegramUser.setCurState(TelegramUser.UserState.FIRST_LANGUAGE_CONFIGURE);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setLangKeyboard(SendMessage sendMessage) {
        sendMessage.setReplyMarkup(kf.getKeyboard(LANG_KEYBOARD, "") //lang iso empty because currently we have no lang, get default bundle
                .setResizeKeyboard(true));
    }
}
