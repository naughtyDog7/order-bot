package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.ResourceBundle;

public interface BadRequestHandler {
    void handleTextBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb);

    void handleContactBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb);

    void handleLocationBadRequest(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb);

    void handleRestaurantClosed(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb);

    void handleBadPhoneNumber(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb);
}
