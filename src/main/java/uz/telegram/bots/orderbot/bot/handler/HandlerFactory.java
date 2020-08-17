package uz.telegram.bots.orderbot.bot.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.handler.callback.CallbackHandler;
import uz.telegram.bots.orderbot.bot.handler.invoice.InvoiceHandler;
import uz.telegram.bots.orderbot.bot.handler.message.MessageHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

@Component
public class HandlerFactory {

    private final Map<Class<? extends Handler>, Handler> handlersMap;
    private boolean botSet = false;

    @Autowired
    public HandlerFactory(List<Handler> handlers) {
        handlersMap = handlers.stream().collect(toMap(Handler::getClass, v -> v));
    }

    public void setBot(TelegramLongPollingBot bot) {
        handlersMap.values().forEach(handler -> handler.setBot(bot));
        botSet = true;
    }

    public Optional<Handler> getHandler(Update update) {
        if (!botSet)
            throw new IllegalStateException("Bot is not specified");

        Handler handler = null;
        if (update.hasCallbackQuery()) {
            handler = handlersMap.get(CallbackHandler.class);
        } else if (update.hasMessage()) {
            handler = handlersMap.get(MessageHandler.class);
        } else if (update.hasPreCheckoutQuery()) {
            handler = handlersMap.get(InvoiceHandler.class);
        }
        return Optional.ofNullable(handler);
    }
}
