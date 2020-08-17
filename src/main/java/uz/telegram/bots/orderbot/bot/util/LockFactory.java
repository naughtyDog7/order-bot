package uz.telegram.bots.orderbot.bot.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockFactory {
    private final Map<Long, Lock> locks = new HashMap<>();

    public Lock getLock(long chatId) {
        return locks.computeIfAbsent(chatId, key -> new ReentrantLock());
    }
}
