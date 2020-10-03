package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.ResourceBundle;

@Component
public class BadRequestHandlerImpl implements BadRequestHandler {
    public void handleTextBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(user.getChatId())
                .setText(rb.getString("bad-request-message"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleContactBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(user.getChatId())
                .setText(rb.getString("contact-bad-request-message"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleLocationBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(user.getChatId())
                .setText(rb.getString("location-bad-request-message"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleRestaurantClosed(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setText(rb.getString("restaurant-currently-closed"))
                .setChatId(telegramUser.getChatId());
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleBadPhoneNumber(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setText(rb.getString("enter-phone-num-in-format"))
                .setChatId(telegramUser.getChatId());
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
