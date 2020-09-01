package uz.telegram.bots.orderbot.bot.properties;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import uz.telegram.bots.orderbot.bot.user.PaymentInfo;

import java.util.Optional;

@ConfigurationProperties(prefix = "payment")
@ConstructorBinding
@Value
public class PaymentProperties {
    String clickToken;
    String paymeToken;

    public Optional<String> getByPaymentMethod(PaymentInfo.PaymentMethod paymentMethod) {
        String token = null;
        if (paymentMethod == PaymentInfo.PaymentMethod.CLICK) {
            token = clickToken;
        } else if (paymentMethod == PaymentInfo.PaymentMethod.PAYME) {
            token = paymeToken;
        }
        return Optional.ofNullable(token);
    }
}
