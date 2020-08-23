package uz.telegram.bots.orderbot.bot.handler.message;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.handler.Handler;
import uz.telegram.bots.orderbot.bot.handler.message.state.MessageState;
import uz.telegram.bots.orderbot.bot.handler.message.state.MessageStateFactory;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.LockFactory;

import java.util.concurrent.locks.Lock;

@Component
@Slf4j
public class MessageHandler implements Handler {

    @Setter
    private TelegramLongPollingBot bot;

    private final MessageStateFactory msf;
    private final TelegramUserService service;
    private final LockFactory rlf;

    @Autowired
    public MessageHandler(MessageStateFactory msf, TelegramUserService service, LockFactory rlf) {
        this.msf = msf;
        this.service = service;
        this.rlf = rlf;
    }

    @Override
    public void handle(Update update) {
        TelegramUser telegramUser = service.getOrSaveUser(update);
        Lock lock = rlf.getLockForChatId(telegramUser.getChatId());
        try {
            TelegramUser updatedUser;
            if (lock.tryLock()) {
                updatedUser = telegramUser;
            } else {
                lock.lock();
                // if unsuccessfully locked, then maybe state changed, so need to update
                updatedUser = service.findById(telegramUser.getId())
                        .orElseThrow(() -> new AssertionError("User must be found at this point"));
            }
            MessageState messageState = msf.getMessageState(update, updatedUser)
                    .orElseThrow(() -> new IllegalStateException("MessageState for state " + telegramUser.getCurState() + " not found."));
            messageState.handle(update, bot, updatedUser);
        } finally {
            lock.unlock();
        }
    }

}
