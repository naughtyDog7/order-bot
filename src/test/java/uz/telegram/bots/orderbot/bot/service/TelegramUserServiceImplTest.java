package uz.telegram.bots.orderbot.bot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uz.telegram.bots.orderbot.bot.config.TestJpaConfig;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TelegramUserServiceImpl.class})
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
class TelegramUserServiceImplTest {

    @Autowired
    private TelegramUserService userService;

    @Test
    void checkAndSetPhoneNum() {
        TelegramUser testUser = new TelegramUser();
        List<String> correctNums = List.of("998-99-123-45-67", "+998-99-123-45-67", "99-123-45-67", "99 123 45 67", "991234567");
        List<String> incorrectNums = List.of("1234", "++998991111111", "1234567", "123-45-67", "99 999 99 999", "10A123");
        for (String correctNum : correctNums) {
            testUser.setPhoneNum(null);
            userService.checkAndSetPhoneNum(testUser, correctNum);
            assertNotNull(testUser.getPhoneNum());
        }
        for (String incorrectNum : incorrectNums) {
            assertThrows(IllegalArgumentException.class, () -> userService.checkAndSetPhoneNum(testUser, incorrectNum));
        }
    }
}