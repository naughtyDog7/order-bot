package uz.telegram.bots.orderbot.bot.util;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class KeyboardUtil {

    private final ResourceBundleFactory rbf;

    @Autowired
    public KeyboardUtil(ResourceBundleFactory rbf) {
        this.rbf = rbf;
    }

    //immutable operation, returns new keyboard with added back button
    public ReplyKeyboardMarkup addBackButtonLast(ReplyKeyboardMarkup keyboard, String langISO) {
        ResourceBundle rb = rbf.getMessagesBundle(langISO);
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(rb.getString("btn-back"));
        List<KeyboardRow> mutableRows = new ArrayList<>(keyboard.getKeyboard());
        mutableRows.add(keyboardRow);
        return new ReplyKeyboardMarkup(ImmutableList.copyOf(mutableRows));
    }
}
