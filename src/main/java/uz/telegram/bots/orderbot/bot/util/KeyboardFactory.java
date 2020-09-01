package uz.telegram.bots.orderbot.bot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.telegram.bots.orderbot.bot.properties.BotProperties;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeyboardFactory {

    private final Map<String, Map<KeyboardType, ReplyKeyboardMarkup>> keyboardsByLang = new HashMap<>();
    private final KeyboardUtil ku;
    private final ResourceBundleFactory rbf;

    @Autowired
    public KeyboardFactory(BotProperties botProperties, ResourceBundleFactory rbf, KeyboardUtil ku) {
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

    public ReplyKeyboardMarkup getKeyboard(KeyboardType type, String langISO) {
        return keyboardsByLang.getOrDefault(langISO, keyboardsByLang.get(""))
                .get(type);
    }

    public ReplyKeyboardMarkup getBackButtonKeyboard(String langISO) {
        KeyboardRow row = new KeyboardRow();
        ResourceBundle rb = rbf.getMessagesBundle(langISO);
        row.add(rb.getString("btn-back"));
        return new ReplyKeyboardMarkup(List.of(row));
    }

    public enum KeyboardType {
        LANG_KEYBOARD {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardRow keyboardButtons = new KeyboardRow();
                keyboardButtons.add(rb.getString("btn-uzb-lang"));
                keyboardButtons.add(rb.getString("btn-rus-lang"));
                return new ReplyKeyboardMarkup(List.of(keyboardButtons));
            }
        },
        MAIN_MENU_KEYBOARD {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardRow keyboardButtons1 = new KeyboardRow();
                keyboardButtons1.add(rb.getString("btn-main-menu-order"));
                KeyboardRow keyboardButtons2 = new KeyboardRow();
                keyboardButtons2.add(rb.getString("btn-main-menu-settings"));
                keyboardButtons2.add(rb.getString("btn-main-menu-contact-us"));
                return new ReplyKeyboardMarkup(List.of(keyboardButtons1, keyboardButtons2));
            }
        },
        SETTINGS_KEYBOARD {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardRow keyboardButtons = new KeyboardRow();
                keyboardButtons.add(rb.getString("btn-settings-language-choose"));
                return new ReplyKeyboardMarkup(List.of(keyboardButtons));
            }
        },
        CATEGORIES_TEMPLATE_KEYBOARD {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardRow keyboardButtons1 = new KeyboardRow();
                keyboardButtons1.add(rb.getString("btn-basket"));
                KeyboardRow keyboardButtons2 = new KeyboardRow();
                keyboardButtons2.add(rb.getString("btn-cancel-order"));
                return new ReplyKeyboardMarkup(List.of(keyboardButtons1, keyboardButtons2));
            }
        },
        PHONE_NUM_ENTER_KEYBOARD {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardRow row = new KeyboardRow();
                KeyboardButton keyboardButton = new KeyboardButton(rb.getString("btn-send-contact"))
                        .setRequestContact(true);
                row.add(keyboardButton);
                return new ReplyKeyboardMarkup(List.of(row));
            }
        },
        LOCATION_KEYBOARD {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardButton keyboardButton = new KeyboardButton(rb.getString("btn-send-location"))
                        .setRequestLocation(true);
                KeyboardRow keyboardRow = new KeyboardRow();
                keyboardRow.add(keyboardButton);
                return new ReplyKeyboardMarkup(List.of(keyboardRow));
            }
        },
        PAYMENT_METHOD_CHOOSE {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardRow row = new KeyboardRow();
                row.add(rb.getString("btn-cash"));
                row.add(rb.getString("btn-click"));
                row.add(rb.getString("btn-payme"));
                return new ReplyKeyboardMarkup(List.of(row));
            }
        },
        FINAL_CONFIRMATION_KEYBOARD {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardRow row = new KeyboardRow();
                row.add(rb.getString("btn-confirm"));
                return new ReplyKeyboardMarkup(List.of(row));
            }
        },
        CHECK_STATUS_KEYBOARD {
            @Override
            ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb) {
                KeyboardRow row = new KeyboardRow();
                row.add(rb.getString("btn-check-order-status"));
                row.add(rb.getString("btn-cancel-order"));
                return new ReplyKeyboardMarkup(List.of(row));
            }
        };

        abstract ReplyKeyboardMarkup getReplyKeyboard(ResourceBundle rb);
    }
}
