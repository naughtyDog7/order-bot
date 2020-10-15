package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.ResourceBundle;

class JowiServerFailureHandler {
    public static void handleServerFail(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage serverFailureMessage = new SendMessage()
                .setText(rb.getString("server-failure-message"))
                .setChatId(telegramUser.getChatId());

        try {
            bot.execute(serverFailureMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
