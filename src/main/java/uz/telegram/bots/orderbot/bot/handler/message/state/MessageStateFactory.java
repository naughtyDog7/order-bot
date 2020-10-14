package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;


@Component
public class MessageStateFactory {

    private final Map<Class<? extends MessageState>, MessageState> messageStatesMap;

    public MessageStateFactory(List<MessageState> messageStatesList) {
        messageStatesMap = messageStatesList.stream().collect(toMap(MessageState::getClass, ms -> ms));
    }

    public Optional<MessageState> getMessageState(Update update, TelegramUser telegramUser) {
        Class<? extends MessageState> stateHandlerClass
                = telegramUser.getCurState().getStateHandlerClass();
        MessageState ms = messageStatesMap.get(stateHandlerClass);
        return Optional.ofNullable(ms);
    }
}
