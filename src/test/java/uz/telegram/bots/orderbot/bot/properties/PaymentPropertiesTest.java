package uz.telegram.bots.orderbot.bot.properties;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@EnableConfigurationProperties(PaymentProperties.class)
@TestInstance(PER_CLASS)
@Disabled("Currently payment methods in app disabled")
class PaymentPropertiesTest {

    @Autowired
    private PaymentProperties paymentProperties;

    @Test
    void getClickToken() {
        assertNotNull(paymentProperties.getClickToken());
    }

    @Test
    void getPaymeToken() {
        assertNotNull(paymentProperties.getPaymeToken());
    }
}