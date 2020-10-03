package uz.telegram.bots.orderbot.bot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.telegram.bots.orderbot.bot.properties.BotProperties;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeyboardFactoryImpl implements KeyboardFactory {

    private final Map<String, Map<KeyboardType, ReplyKeyboardMarkup>> keyboardsByLang = new HashMap<>();
    private final KeyboardUtil ku;
    private final ResourceBundleFactory rbf;

    @Autowired
    public KeyboardFactoryImpl(BotProperties botProperties, ResourceBundleFactory rbf, KeyboardUtil ku) {
        this.rbf = rbf;
        this.ku = ku;
        List<String> langs = botProperties.getUsedLanguages();
        langs.add(""); //add empty string so it will load default resource bundle
        for (String lang : langs) {
            ResourceBundle rb = rbf.getMessagesBundle(Locale.forLanguageTag(lang));
            EnumMap<KeyboardType, ReplyKeyboardMarkup> keyboards =
                    EnumSet.allOf(KeyboardType.class).stream()
                            .collect(Collectors.toMap(kt -> kt, kt -> kt.getReplyKeyboard(rb),
                                    (kt1, kt2) -> kt2, () -> new EnumMap<>(KeyboardType.class)));
            keyboardsByLang.put(rb.getLocale().getISO3Language(), keyboards);
        }
    }

    @Override
    public ReplyKeyboardMarkup getKeyboard(KeyboardType type, String langISO) {
        return keyboardsByLang.getOrDefault(langISO, keyboardsByLang.get(""))
                .get(type);
    }

    @Override
    public ReplyKeyboardMarkup getBackButtonKeyboard(String langISO) {
        KeyboardRow row = new KeyboardRow();
        ResourceBundle rb = rbf.getMessagesBundle(langISO);
        row.add(rb.getString("btn-back"));
        return new ReplyKeyboardMarkup(List.of(row));
    }

}
