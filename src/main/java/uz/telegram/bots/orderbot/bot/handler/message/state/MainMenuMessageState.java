package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
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
import uz.telegram.bots.orderbot.bot.service.CategoryService;
import uz.telegram.bots.orderbot.bot.service.OrderService;
import uz.telegram.bots.orderbot.bot.service.RestaurantService;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.CONTACT_US;
import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.SETTINGS;

@Component
@Slf4j
class MainMenuMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final RestaurantService restaurantService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final LockFactory lf;

    @Autowired
    MainMenuMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                         RestaurantService restaurantService, KeyboardFactory kf, KeyboardUtil ku,
                         CategoryService categoryService, OrderService orderService, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.restaurantService = restaurantService;
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
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }

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
        else
            DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
    }

    private void handleSettings(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("configure-settings"));
        ku.setSettingsKeyboard(sendMessage, rb, telegramUser.getLangISO(), kf, telegramUser.getPhoneNum() != null);

        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(SETTINGS);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleOrder(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            if (orderService.getActive(telegramUser).isPresent()) {
                SendMessage sendMessage = new SendMessage()
                        .setChatId(telegramUser.getChatId())
                        .setText(rb.getString("double-order-failure"));
                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }

            List<Restaurant> restaurants = restaurantService.updateAndFetchRestaurants();
            Restaurant restaurant = restaurants.get(1); //TODO change to finding closest restaurant impl
            Order order = new Order(telegramUser);
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setFromRestaurant(restaurant);
            order.setPaymentInfo(paymentInfo);
            //test info

            orderService.save(order);

            SendMessage loadingMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("server-loading-message"));
            try {
                Message message = bot.execute(loadingMessage);
                CompletableFuture<List<Category>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return categoryService.updateAndFetchNonEmptyCategories(restaurant.getRestaurantId());
                        // TODO change id to chosen by user restaurant id
                    } catch (IOException e) {
                        JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
                        throw new UncheckedIOException(e);
                    }
                });
                List<Category> categories = future.get(5, TimeUnit.SECONDS);

                DeleteMessage deleteLoadingMessage = new DeleteMessage()
                        .setChatId(telegramUser.getChatId())
                        .setMessageId(message.getMessageId());

                bot.execute(deleteLoadingMessage);
                ToOrderMainHandler.builder()
                        .bot(bot)
                        .telegramUser(telegramUser)
                        .service(userService)
                        .ku(ku)
                        .kf(kf)
                        .rb(rb)
                        .categories(categories)
                        .build()
                        .handleToOrderMain(0, true);
            } catch (TelegramApiException | ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
                log.error("Jowi server timeout");
            }
        } catch (IOException e) {
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
            throw new UncheckedIOException(e);
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
        int size = rows.size();
        if (size > 2 && rows.get(size - 2).size() <= 1 && rows.get(size - 1).size() <= 1)
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
