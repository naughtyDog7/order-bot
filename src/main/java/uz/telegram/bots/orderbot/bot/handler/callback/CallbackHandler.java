package uz.telegram.bots.orderbot.bot.handler.callback;

import lombok.Setter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.handler.Handler;

@Component
public class CallbackHandler implements Handler {

    @Setter
    private TelegramLongPollingBot bot;

    @Override
    public void handle(Update update) {

    }
}
