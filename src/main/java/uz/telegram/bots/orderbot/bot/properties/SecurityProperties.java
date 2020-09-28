package uz.telegram.bots.orderbot.bot.properties;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Value
@ConfigurationProperties("app.security")
@ConstructorBinding
public class SecurityProperties {
    String adminPass;
}
