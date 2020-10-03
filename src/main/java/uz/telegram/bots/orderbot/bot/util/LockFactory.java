package uz.telegram.bots.orderbot.bot.util;

import java.util.concurrent.locks.Lock;

public interface LockFactory {
    Lock getLockForChatId(long chatId);

    Lock getResourceLock();
}
