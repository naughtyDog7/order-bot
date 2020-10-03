package uz.telegram.bots.orderbot.bot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uz.telegram.bots.orderbot.bot.properties.BotProperties;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class ResourceBundleFactoryImpl implements ResourceBundleFactory {
    private final BotProperties botProperties;

    private final Map<String, ResourceBundle> messagesResourceBundles = new HashMap<>();

    @Autowired
    public ResourceBundleFactoryImpl(BotProperties botProperties) {
        this.botProperties = botProperties;
        System.out.println("");
    }

    @PostConstruct
    public void initBundles() {
        for (String langTag : botProperties.getUsedLanguages()) {
            Locale locale = Locale.forLanguageTag(langTag);
            if (locale.toLanguageTag().isBlank())
                throw new IllegalStateException("Locale for language tag: " + langTag + " not found");
            messagesResourceBundles.put(locale.getISO3Language(), ResourceBundle.getBundle("messages", locale));
        }
        messagesResourceBundles.put("", ResourceBundle.getBundle("messages", Locale.ROOT));

    }

    @Override
    public ResourceBundle getDefaultMessageBundle() {
        return messagesResourceBundles.get("");
    }

    @Override
    public ResourceBundle getMessagesBundle(String langISO) {
        if (!messagesResourceBundles.containsKey(langISO))
            throw new IllegalStateException("Can't find resource bundle for lang " + langISO);
        return messagesResourceBundles.get(langISO);
    }

    @Override
    public ResourceBundle   getMessagesBundle(Locale locale) {
        String langISO = locale.getISO3Language();
        return getMessagesBundle(langISO);
    }
}
