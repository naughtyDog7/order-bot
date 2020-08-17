package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

public interface MessageState {
    void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser);
}
