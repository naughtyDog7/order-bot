package uz.telegram.bots.orderbot.bot.properties;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.List;

@ConfigurationProperties(prefix = "bot")
@ConstructorBinding
@Value
public class BotProperties {
    List<String> usedLanguages;
    String username;
    String botToken;


}
