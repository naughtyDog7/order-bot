package uz.telegram.bots.orderbot.bot.util;

import java.util.Locale;
import java.util.ResourceBundle;

public interface ResourceBundleFactory {
    ResourceBundle getDefaultMessageBundle();

    ResourceBundle getMessagesBundle(String langISO);

    ResourceBundle   getMessagesBundle(Locale locale);
}
