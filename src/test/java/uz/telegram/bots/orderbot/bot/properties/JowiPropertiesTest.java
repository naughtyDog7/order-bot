package uz.telegram.bots.orderbot.bot.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@EnableConfigurationProperties(JowiProperties.class)
@TestInstance(PER_CLASS)
class JowiPropertiesTest {

    @Autowired
    private JowiProperties props;

    @Test
    void getApiUrlV010() {
        assertDoesNotThrow(() -> new URL(props.getApiUrlV010()));
    }

    @Test
    void getApiUrlV3() {
        assertDoesNotThrow(() -> new URL(props.getApiUrlV3()));
    }

    @Test
    void getApiKey() {
        assertNotNull(props.getApiKey());
    }

    @Test
    void getSig() {
        assertNotNull(props.getSig());
    }
}