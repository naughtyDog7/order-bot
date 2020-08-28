package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.Builder;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.CATEGORY_MAIN;

@Builder
class ToCategoryMainHandler {
    private final TelegramLongPollingBot bot;
    private final TelegramUser telegramUser;
    private final ResourceBundle rb;
    private final TelegramUserService userService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final List<Product> products;

    void handleToProducts() {
        Objects.requireNonNull(bot);
        Objects.requireNonNull(telegramUser);
        Objects.requireNonNull(rb);
        Objects.requireNonNull(userService);
        Objects.requireNonNull(kf);
        Objects.requireNonNull(ku);
        Objects.requireNonNull(products);

        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("choose-course"));

        setProductsKeyboard(sendMessage, telegramUser.getLangISO(), products);

        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(CATEGORY_MAIN);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setProductsKeyboard(SendMessage sendMessage, String langISO, List<Product> products) {
        List<KeyboardRow> rows = new ArrayList<>();
        List<Product> filteredProducts = products.stream().filter(p -> p.getCountLeft() > 0).collect(Collectors.toList());
        ku.addProductsToRows(rows, filteredProducts, 0);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(rows);
        keyboard = ku.addBackButtonLast(keyboard, langISO);
        if (rows.get(rows.size() - 1).size() <= 1)
            keyboard = ku.concatLastTwoRows(keyboard);
        sendMessage.setReplyMarkup(keyboard
                .setResizeKeyboard(true));
    }
}
