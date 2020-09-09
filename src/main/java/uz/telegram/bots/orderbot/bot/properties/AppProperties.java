package uz.telegram.bots.orderbot.bot.properties;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "app")
@ConstructorBinding
@Value
public class AppProperties {
    int basketCountLimit;
    int freeDeliveryLowerBound;
    int deliveryPrice;
}
