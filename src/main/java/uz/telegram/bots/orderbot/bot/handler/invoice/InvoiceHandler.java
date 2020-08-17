package uz.telegram.bots.orderbot.bot.handler.invoice;

import lombok.Setter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.handler.Handler;

@Component
public class InvoiceHandler implements Handler {

    @Setter
    private TelegramLongPollingBot bot;

    @Override
    public void handle(Update update) {

    }
}
