package uz.telegram.bots.orderbot.bot.util;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.telegram.bots.orderbot.bot.service.ProductService;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.SETTINGS_KEYBOARD;

@Component
public class KeyboardUtil {

    private final ResourceBundleFactory rbf;

    @Autowired
    public KeyboardUtil(ResourceBundleFactory rbf) {
        this.rbf = rbf;
    }

    //immutable operation, returns new keyboard with added back button
    public ReplyKeyboardMarkup addBackButtonLast(ReplyKeyboardMarkup keyboard, String langISO) {
        return addBackButtonLast(keyboard.getKeyboard(), langISO);
    }

    public ReplyKeyboardMarkup addBackButtonLast(List<KeyboardRow> keyboardRows, String langISO) {
        ResourceBundle rb = rbf.getMessagesBundle(langISO);
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(rb.getString("btn-back"));
        List<KeyboardRow> mutableRows = new ArrayList<>(keyboardRows);
        mutableRows.add(keyboardRow);
        return new ReplyKeyboardMarkup(ImmutableList.copyOf(mutableRows));
    }

    /**
     * If both last and pre last rows contain 1 element, concats them, and returns new ReplyKeyboardMarkup
     *
     * @param replyKeyboardMarkup initial keyboard
     * @return keyboard with concatenated last 2 rows, if they contain 1 element
     */
    public ReplyKeyboardMarkup concatLastTwoRows(ReplyKeyboardMarkup replyKeyboardMarkup) {
        List<KeyboardRow> rows = new ArrayList<>(replyKeyboardMarkup.getKeyboard());

        return concatLastTwoRows(rows);
    }

    public ReplyKeyboardMarkup concatLastTwoRows(List<KeyboardRow> rows) {
        if (rows.size() < 2)
            throw new IllegalArgumentException("Keyboard should contain at least 2 rows");

        KeyboardRow preLastRow = rows.get(rows.size() - 2);
        KeyboardRow lastRow = rows.get(rows.size() - 1);

        preLastRow.addAll(lastRow);
        rows.remove(lastRow);
        return new ReplyKeyboardMarkup(ImmutableList.copyOf(rows));
    }


    public void setProductNumChoose(SendMessage sendMessage, String langISO, Product product) {
        int countLeft = product.getCountLeft();
        int buttonsNum = Math.min(countLeft, 9); //default num is 9 buttons, but if less than 9 left, then countLeft buttons

        int buttonsInFullRows = buttonsNum - buttonsNum % 3;
        List<KeyboardRow> rows = new ArrayList<>();
        for (int i = 1; i < buttonsInFullRows + 1; i += 3) {
            KeyboardRow keyboardRow = new KeyboardRow();
            for (int j = 0; j < 3; j++) {
                keyboardRow.add(String.valueOf(i + j));
            }
            rows.add(keyboardRow);
        }
        {
            KeyboardRow keyboardRow = new KeyboardRow();
            for (int i = buttonsInFullRows + 1; i < buttonsNum + 1; i++) {
                keyboardRow.add(String.valueOf(i));
            }
            rows.add(keyboardRow);
        }
        ReplyKeyboardMarkup keyboard = addBackButtonLast(rows, langISO);
        int size = keyboard.getKeyboard().size();
        if (size >= 2) {
            KeyboardRow preLastRow = keyboard.getKeyboard().get(size - 2);
            KeyboardRow lastRow = keyboard.getKeyboard().get(size - 1);
            if (preLastRow.size() <= 2 && lastRow.size() == 1)
                keyboard = concatLastTwoRows(keyboard);
        }
        sendMessage.setReplyMarkup(keyboard
                .setResizeKeyboard(true));
    }

    public void addCategoriesToRows(List<KeyboardRow> rows, List<Category> categories, int rowStartIndex) {
        for (int i = 0; i < categories.size(); i++) {
            KeyboardRow keyboardButtons = new KeyboardRow();
            keyboardButtons.add(categories.get(i).getName());
            if (i + 1 < categories.size()) {
                keyboardButtons.add(categories.get(++i).getName());
            }
            rows.add(rowStartIndex++, keyboardButtons);
        }
    }

    public void addProductsToRows(List<KeyboardRow> rows, List<Product> products, int rowStartIndex) {
        for (int i = 0; i < products.size(); i++) {
            KeyboardRow keyboardButtons = new KeyboardRow();
            keyboardButtons.add(products.get(i).getName());
            if (i + 1 < products.size()) {
                keyboardButtons.add(products.get(++i).getName());
            }
            rows.add(rowStartIndex++, keyboardButtons);
        }
    }

    public void setBasketKeyboard(ProductService productService, SendMessage sendMessage, List<ProductWithCount> products, ResourceBundle rb, String langISO) {
        List<KeyboardRow> rows = new ArrayList<>();
        products.forEach(p -> {
            KeyboardRow keyboardRow = new KeyboardRow();
            Product product = productService.fromProductWithCount(p.getId());
            keyboardRow.add(product.getName() + " " + rb.getString("remove-product-char"));
            rows.add(keyboardRow);
        });
        sendMessage.setReplyMarkup(addBackButtonLast(rows, langISO)
                .setResizeKeyboard(true));
    }

    public void setSettingsKeyboard(SendMessage sendMessage, ResourceBundle rb, String langISO, KeyboardFactory kf, boolean phoneNumExists) {
        List<KeyboardRow> rows = new ArrayList<>(kf.getKeyboard(SETTINGS_KEYBOARD, langISO).getKeyboard());
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.addAll(rows.get(0));
        if (phoneNumExists) {
            firstRow.add(rb.getString("btn-change-existing-phone-num"));
        } else {
            firstRow.add(rb.getString("btn-set-new-phone-num"));
        }
        rows.set(0, firstRow);
        sendMessage.setReplyMarkup(addBackButtonLast(rows, langISO)
                .setResizeKeyboard(true));
    }
}
