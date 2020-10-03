package uz.telegram.bots.orderbot.bot.util;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockFactoryImpl implements LockFactory {
    private final Map<Long, Lock> locks = new HashMap<>();

    @Override
    public Lock getLockForChatId(long chatId) {
        return locks.computeIfAbsent(chatId, key -> new ReentrantLock(true));
    }

    @Getter
    private final Lock resourceLock = new ReentrantLock(true);
}
