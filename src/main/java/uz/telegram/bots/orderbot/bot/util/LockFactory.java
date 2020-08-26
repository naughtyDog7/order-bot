package uz.telegram.bots.orderbot.bot.util;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockFactory {
    private final Map<Long, Lock> locks = new HashMap<>();

    public Lock getLockForChatId(long chatId) {
        return locks.computeIfAbsent(chatId, key -> new ReentrantLock(true));
    }

    @Getter
    private final Lock resourceLock = new ReentrantLock(true);
}
