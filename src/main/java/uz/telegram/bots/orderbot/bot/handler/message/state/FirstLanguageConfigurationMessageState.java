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

import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.MAIN_MENU_KEYBOARD;

@Component
class FirstLanguageConfigurationMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    FirstLanguageConfigurationMessageState(ResourceBundleFactory rbf, TelegramUserService service,
                                           KeyboardFactory kf, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //setting user language, saving to db, and sending success message and main menu message
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        if (!message.hasText()) {
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rbf.getDefaultMessageBundle());
            return;
        }

        ResourceBundle defaultMessageBundle = rbf.getDefaultMessageBundle();
        String lang = message.getText();

        boolean defaultChosen = false;

        if (lang.contains(defaultMessageBundle.getString("btn-uzb-lang")))
            telegramUser.setLangISO("uzb");
        else if (lang.contains(defaultMessageBundle.getString("btn-rus-lang")))
            telegramUser.setLangISO("rus");
        else {
            defaultChosen = true; //If user enters invalid lang name default is chosen, and answer message is changed to "default lang chosen"
            telegramUser.setLangISO("rus");
        }
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        String successText = !defaultChosen ? rb.getString("language-chosen")
                : "Язык " + lang + " не поддержтвается\n" + rb.getString("default-lang-chosen");

        SendMessage sendMessage1 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(successText);

        SendMessage sendMessage2 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("main-menu-message"));

        setMenuKeyboard(sendMessage2, telegramUser.getLangISO());
        try {
            bot.execute(sendMessage1);
            bot.execute(sendMessage2);
            telegramUser.setCurState(TelegramUser.UserState.MAIN_MENU);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        service.save(telegramUser); //saving here cause we updated lang
    }

    private void setMenuKeyboard(SendMessage sendMessage, String langISO) {
        sendMessage.setReplyMarkup(kf.getKeyboard(MAIN_MENU_KEYBOARD, langISO)
                .setResizeKeyboard(true));
    }
}
