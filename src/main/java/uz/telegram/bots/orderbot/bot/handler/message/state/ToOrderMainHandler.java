package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.Builder;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.handler.message.state.UserState.ORDER_MAIN;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.CATEGORIES_TEMPLATE_KEYBOARD;
import static uz.telegram.bots.orderbot.bot.util.TextUtil.getRandMealEmoji;

@Builder
class ToOrderMainHandler {
    private final TelegramLongPollingBot bot;
    private final TelegramUser telegramUser;
    private final ResourceBundle rb;
    private final TelegramUserService service;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final List<Category> categories;

    void handleToOrderMain(int basketNumItems, CallerPlace callerPlace) {
        Objects.requireNonNull(bot);
        Objects.requireNonNull(telegramUser);
        Objects.requireNonNull(rb);
        Objects.requireNonNull(service);
        Objects.requireNonNull(kf);
        Objects.requireNonNull(ku);
        Objects.requireNonNull(categories);

        if (categories.isEmpty()) {
            handleEmptyCategories(basketNumItems);
        } else {
            String text = rb.getString(callerPlace == CallerPlace.MAIN_MENU
                    ? "order-message" : "order-message-2");
            SendMessage sendMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(text + getRandMealEmoji());
            setKeyboard(sendMessage, basketNumItems);

            try {
                bot.execute(sendMessage);
                telegramUser.setCurState(ORDER_MAIN);
                service.save(telegramUser);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleEmptyCategories(int basketNumItems) {
        if (basketNumItems == 0) {
            SendMessage sendMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("currently-no-available-courses"));
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            SendMessage sendMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("no-non-empty-categories-left"));
            setKeyboard(sendMessage, basketNumItems);

            try {
                bot.execute(sendMessage);
                telegramUser.setCurState(ORDER_MAIN);
                service.save(telegramUser);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void setKeyboard(SendMessage sendMessage, int basketNumItems) {
        List<KeyboardRow> rows = new ArrayList<>(kf.getKeyboard(CATEGORIES_TEMPLATE_KEYBOARD, rb.getLocale().getISO3Language())
                .getKeyboard());

        String newBasketText = rows.get(0).get(0).getText().concat("(" + basketNumItems + ")");
        KeyboardRow newFirstRow = new KeyboardRow();
        newFirstRow.add(newBasketText);
        if (basketNumItems > 0) {
            newFirstRow.add(0, rb.getString("btn-order-main"));
        }
        rows.set(0, newFirstRow);
        ku.addCategoriesToRows(rows, categories, 1);
        ReplyKeyboardMarkup keyboard;
        KeyboardRow preLastRow = rows.get(rows.size() - 2);
        KeyboardRow lastRow = rows.get(rows.size() - 1);
        if (rows.size() > 2 && preLastRow.size() <= 1 && lastRow.size() <= 1)
            keyboard = ku.concatLastTwoRows(rows);
        else
            keyboard = new ReplyKeyboardMarkup(rows);

        sendMessage.setReplyMarkup(keyboard
                .setResizeKeyboard(true));
    }

    enum CallerPlace {
        MAIN_MENU,
        OTHER
    }
}
