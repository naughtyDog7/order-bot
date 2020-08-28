package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.ORDER_PHONE_NUM;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.PHONE_NUM_ENTER_KEYBOARD;

@Component
@Slf4j
class OrderMainMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final RestaurantService restaurantService;
    private final ProductService productService;
    private final ProductWithCountService productWithCountService;
    private final LockFactory lf;
    private final TextUtil tu;

    @Autowired
    OrderMainMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                          KeyboardFactory kf, KeyboardUtil ku, CategoryService categoryService,
                          OrderService orderService, RestaurantService restaurantService,
                          ProductService productService, ProductWithCountService productWithCountService,
                          LockFactory lf, TextUtil tu) {
        this.rbf = rbf;
        this.userService = userService;
        this.kf = kf;
        this.ku = ku;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.restaurantService = restaurantService;
        this.productService = productService;
        this.productWithCountService = productWithCountService;
        this.lf = lf;
        this.tu = tu;
    }

    @Override
    //can come as Order btn, basket, category, cancel order
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }

        String text = message.getText();
        Order order = orderService.getActive(telegramUser)
                .orElseThrow(() -> new AssertionError("Order must be present at this point"));
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Pattern basketPattern = getPattern(rb, telegramUser.getLangISO());
            if (basketPattern.matcher(text).matches()) {
                handleBasket(bot, telegramUser, rb, order);
                return;
            } else if (text.equals(rb.getString("btn-order-main"))) {
                handleOrder(bot, telegramUser, rb);
                return;
            } else if (text.equals(rb.getString("btn-cancel-order"))) {
                handleCancel(bot, telegramUser, rb, order);
                return;
            }

            Restaurant restaurant = restaurantService.getByOrderId(order.getId());

            List<Category> categories = categoryService.updateAndFetchNonEmptyCategories(restaurant.getRestaurantId());
            int index = Category.getNames(categories).indexOf(text);
            if (index != -1)
                handleCategory(bot, telegramUser, rb,
                        categories.get(index), order);
            else
                DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
        } catch (IOException e) {
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
            throw new UncheckedIOException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * this map created to cache basket patterns for lang iso
     * and dont waste time to create pattern each time String.mathces() called
     */
    private static final Map<String, Pattern> BASKET_PATTERNS = new HashMap<>();

    private static Pattern getPattern(ResourceBundle rb, String langISO) {
        return BASKET_PATTERNS.computeIfAbsent(langISO, (key) -> Pattern.compile(rb.getString("btn-basket") + "\\(\\d+\\)"));
    }

    private void handleCancel(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        orderService.cancelOrder(order);
        ToMainMenuHandler.builder()
                .bot(bot)
                .kf(kf)
                .rb(rb)
                .service(userService)
                .telegramUser(telegramUser)
                .build()
                .handleToMainMenu();
    }

    private void handleCategory(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                ResourceBundle rb, Category category, Order order) {

        List<Product> products = productService.getAllByCategoryId(category.getId());

        ToCategoryMainHandler.builder()
                .bot(bot)
                .userService(userService)
                .telegramUser(telegramUser)
                .rb(rb)
                .kf(kf)
                .ku(ku)
                .products(products)
                .build()
                .handleToProducts();

        order.setChosenCategoryName(category.getName());
        orderService.save(order);
    }


    private void handleOrder(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId());
        if (telegramUser.getPhoneNum() == null) {
            sendMessage.setText(rb.getString("start-execute-order-no-phone") + "\n\n" + rb.getString("press-to-send-contact"));
            setNewPhoneKeyboard(sendMessage, telegramUser.getLangISO());
        } else {
            sendMessage.setText(rb.getString("start-execute-order-with-phone")
                    .replace("{phoneNum}", telegramUser.getPhoneNum()));
            setConfirmPhoneKeyboard(sendMessage, rb, telegramUser.getLangISO());
        }
        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(ORDER_PHONE_NUM);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setNewPhoneKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(PHONE_NUM_ENTER_KEYBOARD, langISO);
        sendMessage.setReplyMarkup(
                ku.addBackButtonLast(keyboard, langISO)
                        .setResizeKeyboard(true));
    }

    private void setConfirmPhoneKeyboard(SendMessage sendMessage, ResourceBundle rb, String langISO) {
        KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add(rb.getString("btn-confirm-phone"));
        keyboardButtons.add(rb.getString("btn-change-existing-phone-num"));
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(keyboardButtons);
        sendMessage.setReplyMarkup(ku.addBackButtonLast(rows, langISO)
                .setResizeKeyboard(true));
    }

    private void handleBasket(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        List<ProductWithCount> products = productWithCountService.getAllFromOrderId(order.getId());
        if (products.isEmpty()) {
            handleEmptyBasket(bot, telegramUser, rb);
            return;
        }
        String text = tu.appendProducts(products, rb);
        SendMessage sendMessage = new SendMessage()
                .setText(text)
                .setChatId(telegramUser.getChatId());

        ku.setBasketKeyboard(productService, sendMessage, products, rb, telegramUser.getLangISO());

        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(TelegramUser.UserState.BASKET_MAIN);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleEmptyBasket(TelegramLongPollingBot bot, TelegramUser user, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(user.getChatId())
                .setText(rb.getString("basket-empty-message"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
