package uz.telegram.bots.orderbot.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.handler.Handler;
import uz.telegram.bots.orderbot.bot.handler.HandlerFactory;
import uz.telegram.bots.orderbot.bot.properties.AppProperties;
import uz.telegram.bots.orderbot.bot.properties.BotProperties;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;
import uz.telegram.bots.orderbot.bot.properties.PaymentProperties;
import uz.telegram.bots.orderbot.bot.util.ThreadFactoryImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component
@EnableConfigurationProperties({BotProperties.class, JowiProperties.class, PaymentProperties.class, AppProperties.class})
public class OrderBot extends TelegramLongPollingBot {

    private final ExecutorService executor = Executors.newFixedThreadPool(2, new ThreadFactoryImpl());
    private final HandlerFactory handlerFactory;
    private final BotProperties botProperties;

    @Autowired
    public OrderBot(HandlerFactory handlerFactory, BotProperties botProperties) {
        this.handlerFactory = handlerFactory;
        this.botProperties = botProperties;
        handlerFactory.setBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        Handler handler = handlerFactory.getHandler(update)
                .orElseThrow(() -> new IllegalStateException("Couldn't find handler for update:\n " + update));
        executor.execute(() -> handler.handle(update));
    }


    @Override
    public String getBotToken() {
        return botProperties.getBotToken();
    }

    @Override
    public String getBotUsername() {
        return botProperties.getUsername();
    }
}
