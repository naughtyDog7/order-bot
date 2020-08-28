package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.ResourceBundle;

public class DefaultBadRequestHandler {
    public static void handleTextBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(user.getChatId())
                .setText(rb.getString("bad-request-message"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void handleContactBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(user.getChatId())
                .setText(rb.getString("contact-bad-request-message"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void handleLocationBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(user.getChatId())
                .setText(rb.getString("location-bad-request-message"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
