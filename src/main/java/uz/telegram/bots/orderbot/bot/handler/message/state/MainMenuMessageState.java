package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.*;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.SETTINGS_KEYBOARD;
import static uz.telegram.bots.orderbot.bot.util.TextUtil.getRandMealEmoji;

@Component
class MainMenuMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final RestaurantService restaurantService;
    private final PaymentInfoService paymentInfoService;
    private final LocationService locationService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final LockFactory lf;

    @Autowired
    MainMenuMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                         RestaurantService restaurantService, PaymentInfoService paymentInfoService,
                         LocationService locationService, KeyboardFactory kf, KeyboardUtil ku,
                         CategoryService categoryService, OrderService orderService, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.paymentInfoService = paymentInfoService;
        this.locationService = locationService;
        this.kf = kf;
        this.ku = ku;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.lf = lf;
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
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();

            List<Restaurant> restaurants = restaurantService.updateAndFetchRestaurants();
            Restaurant restaurant = restaurants.get(0); //TODO change to finding closest restaurant impl
            Order order = new Order(telegramUser);
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setFromRestaurant(restaurant);
            Location location = locationService.save(Location.of(42.2, 68.8));
            paymentInfo.setOrderLocation(location);
            paymentInfo = paymentInfoService.save(paymentInfo);
            order.setPaymentInfo(paymentInfo);
            //test info

            orderService.save(order);

            SendMessage loadingMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("server-loading-message"));
            try {
                Message message = bot.execute(loadingMessage);
                SendMessage sendMessage = new SendMessage()
                        .setChatId(telegramUser.getChatId())
                        .setText(rb.getString("order-message") + getRandMealEmoji());

                // TODO change id to chosen by user restaurant id
                List<Category> categories = categoryService.updateAndFetchCategories(restaurant.getRestaurantId());
                setOrderKeyboard(sendMessage, telegramUser.getLangISO(), categories);

                DeleteMessage deleteLoadingMessage = new DeleteMessage()
                        .setChatId(telegramUser.getChatId())
                        .setMessageId(message.getMessageId());

                bot.execute(deleteLoadingMessage);
                bot.execute(sendMessage);
                telegramUser.setCurState(ORDER_MAIN);
                userService.save(telegramUser);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    //this methods gets standard CATEGORIES_TEMPLATE_KEYBOARD and adds all categories in 2 rows between basket and cancel buttons
    private void setOrderKeyboard(SendMessage sendMessage, String langISO, List<Category> categories) {
        List<KeyboardRow> rows = new ArrayList<>(kf.getKeyboard(KeyboardFactory.KeyboardType.CATEGORIES_TEMPLATE_KEYBOARD, langISO)
                .getKeyboard());
        ku.addCategoriesToRows(rows, categories, 1);

        String newBasketText = rows.get(0).get(0).getText().concat("(0)"); //setBasketItemsNum as zero
        KeyboardRow newFirstRow = new KeyboardRow();
        newFirstRow.add(newBasketText);
        rows.set(0, newFirstRow);
        //doing this to not change keyboard from kf

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(rows);
        keyboard = ku.concatLastTwoRows(keyboard);
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
