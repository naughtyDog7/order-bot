package uz.telegram.bots.orderbot.bot.util;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

@Slf4j
public class ThreadFactoryImpl implements ThreadFactory {
    @Override
    public Thread newThread(@NotNull Runnable r) {
        Thread thread = new Thread(r);
        thread.setUncaughtExceptionHandler(new ExceptionHandler());
        return thread;
    }

    static class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            log.error("Uncaught exception, thread " + t.getName());
            e.printStackTrace();
        }
    }
}
