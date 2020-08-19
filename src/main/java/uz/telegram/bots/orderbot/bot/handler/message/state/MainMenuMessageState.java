package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.CategoryService;
import uz.telegram.bots.orderbot.bot.service.OrderService;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.OrderU;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.*;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.SETTINGS_KEYBOARD;

@Component
class MainMenuMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final CategoryService categoryService;
    private final OrderService orderService;

    @Autowired
    MainMenuMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                         KeyboardFactory kf, KeyboardUtil ku,
                         CategoryService categoryService, OrderService orderService) {
        this.rbf = rbf;
        this.userService = userService;
        this.kf = kf;
        this.ku = ku;
        this.categoryService = categoryService;
        this.orderService = orderService;
    }

    @Override
    //Message to this state can come as Order, Settings, Contact Us
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        if (!message.hasText())
            return;
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        String settings = rb.getString("btn-main-menu-settings");
        String order = rb.getString("btn-main-menu-order");
        String contactUs = rb.getString("btn-main-menu-contact-us");

        String messageText = message.getText();

        if (messageText.equals(settings))
            handleSettings(bot, telegramUser, rb);
        else if (messageText.equals(order))
            handleOrder(bot, telegramUser, rb);
        else if (messageText.equals(contactUs))
            handleContactUs(bot, telegramUser, rb);
    }

    private void handleSettings(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("configure-settings"));
        setSettingsKeyboard(sendMessage, telegramUser.getLangISO());
        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(SETTINGS);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setSettingsKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(SETTINGS_KEYBOARD, langISO)
                .setResizeKeyboard(true);
        keyboard = ku.addBackButtonLast(keyboard, langISO) //if no other buttons, use KeyboardFactory#getBackButtonKeyboard
                .setResizeKeyboard(true);
        sendMessage.setReplyMarkup(keyboard);
    }

    private void handleOrder(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        List<Category> categories = categoryService.fetchCategories("test-restaurant-id"); // TODO fetch products from juwi api
        OrderU order = new OrderU(telegramUser);
        orderService.save(order);

        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("order-message"));

        setOrderKeyboard(sendMessage, telegramUser.getLangISO(), categories);

        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(ORDER_MAIN);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //this methods gets standard CATEGORIES_TEMPLATE_KEYBOARD and adds all categories in 2 rows between basket and cancel buttons
    private void setOrderKeyboard(SendMessage sendMessage, String langISO, List<Category> categories) {
        List<KeyboardRow> mutableList = new ArrayList<>(kf.getKeyboard(KeyboardFactory.KeyboardType.CATEGORIES_TEMPLATE_KEYBOARD, langISO)
                .getKeyboard());
        System.out.println(categories);
        for (int i = 0, rowIndex = 1; i < categories.size(); i++) {
            KeyboardRow keyboardButtons = new KeyboardRow();
            keyboardButtons.add(categories.get(i).getName());
            if (i + 1 < categories.size()) {
                i++;
                keyboardButtons.add(categories.get(i).getName());
            }
            mutableList.add(rowIndex++, keyboardButtons);
        }
        System.out.println(mutableList);
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(mutableList);
        sendMessage.setReplyMarkup(keyboard
                .setResizeKeyboard(true));
    }

    private void handleContactUs(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("contact-us-message"));
        setContactUsKeyboard(sendMessage, telegramUser.getLangISO());
        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(CONTACT_US);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setContactUsKeyboard(SendMessage sendMessage, String langISO) {
        sendMessage.setReplyMarkup(kf.getBackButtonKeyboard(langISO)
                .setResizeKeyboard(true));
    }
}
