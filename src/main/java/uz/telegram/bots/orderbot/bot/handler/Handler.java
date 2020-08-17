package uz.telegram.bots.orderbot.bot.handler;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface Handler {
    void handle(Update update);
    void setBot(TelegramLongPollingBot bot);
}
