package uz.telegram.bots.orderbot.bot.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@EnableConfigurationProperties(AppProperties.class)
@TestInstance(PER_CLASS)
class AppPropertiesTest {

    @Autowired
    private AppProperties props;

    @Test
    void getBasketCountLimit() {
        assertThat(props.getBasketCountLimit(), greaterThan(0));
    }

    @Test
    void getFreeDeliveryLowerBound() {
        assertThat(props.getFreeDeliveryLowerBound(), greaterThanOrEqualTo(0));
    }

    @Test
    void getDeliveryPrice() {
        assertThat(props.getDeliveryPrice(), greaterThanOrEqualTo(0));
    }
}