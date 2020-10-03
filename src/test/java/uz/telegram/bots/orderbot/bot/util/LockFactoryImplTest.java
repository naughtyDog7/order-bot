package uz.telegram.bots.orderbot.bot.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class LockFactoryImplTest {

    private LockFactory lf;

    @BeforeAll
    void setup() {
        lf = new LockFactoryImpl();
    }

    @Test
    void getLockForChatId() {
        Lock lock1 = lf.getLockForChatId(1);
        assertNotNull(lock1);
        assertSame(lock1, lf.getLockForChatId(1));
        assertNotSame(lock1, lf.getLockForChatId(2));
    }

    @Test
    void getResourceLock() {
        assertNotNull(lf.getResourceLock());
    }
}