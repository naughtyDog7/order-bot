package uz.telegram.bots.orderbot.bot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uz.telegram.bots.orderbot.bot.properties.BotProperties;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ResourceBundleFactoryImpl.class})
@EnableConfigurationProperties(BotProperties.class)
@TestInstance(PER_CLASS)
class ResourceBundleFactoryImplTest {

    @Autowired
    private BotProperties botProperties;
    @Autowired
    private ResourceBundleFactoryImpl rbf;

    @Test
    void getDefaultMessageBundle() {
        assertNotNull(rbf.getDefaultMessageBundle());
    }

    @Test
    void getMessagesBundle() {
        for (String langTags: botProperties.getUsedLanguages()) {
            Locale locale = Locale.forLanguageTag(langTags);
            assertNotNull(rbf.getMessagesBundle(locale));
        }
    }
}