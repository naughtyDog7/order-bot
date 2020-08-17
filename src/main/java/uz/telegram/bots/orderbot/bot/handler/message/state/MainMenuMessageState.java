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
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.CONTACT_US;
import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.SETTINGS;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.SETTINGS_KEYBOARD;

@Component
class MainMenuMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;

    @Autowired
    MainMenuMessageState(ResourceBundleFactory rbf, TelegramUserService service, KeyboardFactory kf, KeyboardUtil ku) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
        this.ku = ku;
    }

    @Override
    //Message to this state can come as Order, Settings, Contact Us
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        if (!message.hasText())
            return;
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        String settings = rb.getString("btn-main-menu-settings");
        String order = rb.getString("btn-main-menu-order");
        String contactUs = rb.getString("btn-main-menu-contact-us");

        String messageText = message.getText();

        if (messageText.equals(settings))
            handleSettings(bot, telegramUser, rb);
        else if (messageText.equals(order))
            handleOrder(bot, telegramUser, rb);
        else if (messageText.equals(contactUs))
            handleContactUs(bot, telegramUser, rb);
    }

    private void handleSettings(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("configure-settings"));
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
        keyboard = ku.addBackButtonLast(keyboard, langISO) //if not other buttons, user KeyboardFactory#getBackButtonKeyboard
                .setResizeKeyboard(true);
        sendMessage.setReplyMarkup(keyboard);
    }

    private void handleOrder(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        List<Category> categories = Collections.emptyList(); // TODO fetch products from juwi api

    }

    private void handleContactUs(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("contact-us-message"));
        setContactUsKeyboard(sendMessage, telegramUser.getLangISO());
        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(CONTACT_US);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setContactUsKeyboard(SendMessage sendMessage, String langISO) {
        sendMessage.setReplyMarkup(kf.getBackButtonKeyboard(langISO)
                .setResizeKeyboard(true));
    }
}
