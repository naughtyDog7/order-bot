package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.ResourceBundle;

public class DefaultBadRequestHandler {
    public static void handleBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(user.getChatId())
                .setText(rb.getString("bad-request-message"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
