package uz.telegram.bots.orderbot.bot.util;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.telegram.bots.orderbot.bot.service.ProductService;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;
import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.util.List;
import java.util.ResourceBundle;

public interface KeyboardUtil {
    //immutable operation, returns new keyboard with added back button
    ReplyKeyboardMarkup addBackButtonLast(ReplyKeyboardMarkup keyboard, String langISO);

    ReplyKeyboardMarkup addBackButtonLast(List<KeyboardRow> keyboardRows, String langISO);

    ReplyKeyboardMarkup concatLastTwoRows(ReplyKeyboardMarkup replyKeyboardMarkup);

    ReplyKeyboardMarkup concatLastTwoRows(List<KeyboardRow> rows);

    void setProductNumChoose(SendMessage sendMessage, String langISO, Product product);

    void addCategoriesToRows(List<KeyboardRow> rows, List<Category> categories, int rowStartIndex);

    void addProductsToRows(List<KeyboardRow> rows, List<Product> products, int rowStartIndex);

    void setBasketKeyboard(ProductService productService, SendMessage sendMessage, List<ProductWithCount> products, ResourceBundle rb, String langISO);

    void setSettingsKeyboard(SendMessage sendMessage, ResourceBundle rb, String langISO, KeyboardFactory kf, boolean phoneNumExists);

    void addRestaurantsToRows(List<KeyboardRow> rows, List<Restaurant> restaurants);
}
