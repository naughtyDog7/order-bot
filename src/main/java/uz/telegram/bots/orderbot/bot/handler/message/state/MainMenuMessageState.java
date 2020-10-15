package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
import uz.telegram.bots.orderbot.bot.service.jowi.JowiService;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static uz.telegram.bots.orderbot.bot.handler.message.state.UserState.*;

@Component
@Slf4j
class MainMenuMessageState implements MessageState {

    private static final ZoneId TASHKENT_ZONE_ID = ZoneId.of("GMT+5");
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final RestaurantService restaurantService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final JowiService jowiService;
    private final OrderService orderService;
    private final LockFactory lf;
    private final TextUtil tu;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    MainMenuMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                         RestaurantService restaurantService, KeyboardFactory kf, KeyboardUtil ku,
                         JowiService jowiService, OrderService orderService, LockFactory lf,
                         TextUtil tu, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.kf = kf;
        this.ku = ku;
        this.jowiService = jowiService;
        this.orderService = orderService;
        this.lf = lf;
        this.tu = tu;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as Order, Settings, Contact Us
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }
        String messageText = message.getText();
        handleMessageText(bot, telegramUser, rb, messageText);
    }

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String messageText) {
        String settings = rb.getString("btn-main-menu-settings");
        String order = rb.getString("btn-main-menu-order");
        String contactUs = rb.getString("btn-main-menu-contact-us");
        if (messageText.equals(settings))
            handleSettings(bot, telegramUser, rb);
        else if (messageText.equals(order))
            handleOrder(bot, telegramUser, rb);
        else if (messageText.equals(contactUs))
            handleContactUs(bot, telegramUser, rb);
        else
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
    }

    private void handleSettings(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage settingsMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("configure-settings"));
        ku.setSettingsKeyboard(settingsMessage, rb, telegramUser.getLangISO(), kf, telegramUser.getPhoneNum() != null);
        try {
            bot.execute(settingsMessage);
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
            if (orderService.findActive(telegramUser).isPresent())
                handleDoubleOrderFailure(bot, telegramUser, rb);
            else
                handleNewOrder(bot, telegramUser, rb);
        } finally {
            lock.unlock();
        }
    }

    private void handleDoubleOrderFailure(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                          ResourceBundle rb) {
        SendMessage doubleOrderFailureMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("double-order-failure"));
        try {
            bot.execute(doubleOrderFailureMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleNewOrder(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                ResourceBundle rb) {
        try {
            Message serverLoadingMessage = sendServerLoadingMessage(bot, telegramUser, rb);
            CompletableFuture<List<Restaurant>> future = getRestaurantsAsync(bot, telegramUser, rb);
            LocalDateTime curTime = LocalDateTime.now(TASHKENT_ZONE_ID);
            List<Restaurant> restaurants = future.get(5, TimeUnit.SECONDS);
            List<Restaurant> workingRestaurants = restaurants
                    .stream()
                    .filter(r -> restaurantService.isOpened(curTime, r))
                    .collect(Collectors.toList());
            deleteLoadingMessage(bot, telegramUser, serverLoadingMessage);
            if (workingRestaurants.size() == 0) {
                handleNoWorkingRestaurants(bot, telegramUser, rb);
            } else {
                handleWorkingRestaurantsAvailable(bot, telegramUser, rb, restaurants, workingRestaurants);
            }
        } catch (TelegramApiException | ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
            log.error("Jowi server timeout");
        }
    }

    private Message sendServerLoadingMessage(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                             ResourceBundle rb) throws TelegramApiException {
        SendMessage loadingMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("server-loading-message"));
        return bot.execute(loadingMessage);
    }

    @NotNull
    private CompletableFuture<List<Restaurant>> getRestaurantsAsync(TelegramLongPollingBot bot,
                                                                    TelegramUser telegramUser, ResourceBundle rb) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return jowiService.fetchAndUpdateRestaurants();
            } catch (IOException e) {
                JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
                throw new UncheckedIOException(e);
            }
        });
    }

    private void deleteLoadingMessage(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                      Message message) throws TelegramApiException {
        DeleteMessage deleteLoadingMessage = new DeleteMessage()
                .setChatId(telegramUser.getChatId())
                .setMessageId(message.getMessageId());
        bot.execute(deleteLoadingMessage);
    }

    private void handleNoWorkingRestaurants(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                            ResourceBundle rb) throws TelegramApiException {
        SendMessage noWorkingRestaurantsMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("no-working-restaurants-for-now"));
        bot.execute(noWorkingRestaurantsMessage);
    }

    private void handleWorkingRestaurantsAvailable(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                                   ResourceBundle rb, List<Restaurant> restaurants,
                                                   List<Restaurant> workingRestaurants) throws TelegramApiException {
        String restaurantMessageText = prepareRestaurantsMessageText(rb, restaurants);
        SendMessage restaurantsMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(restaurantMessageText);
        setRestaurantsKeyboard(restaurantsMessage, workingRestaurants, telegramUser.getLangISO());
        bot.execute(restaurantsMessage);
        telegramUser.setCurState(RESTAURANT_CHOOSE);
        userService.save(telegramUser);
    }

    @NotNull
    private String prepareRestaurantsMessageText(ResourceBundle rb, List<Restaurant> restaurants) {
        StringBuilder text = new StringBuilder(rb.getString("choose-restaurant")).append("\n\n");
        tu.appendRestaurants(text, restaurants, rb);
        return text.toString();
    }

    private void setRestaurantsKeyboard(SendMessage sendMessage, List<Restaurant> restaurants,
                                        String langISO) {
        List<KeyboardRow> rows = new ArrayList<>();
        ku.addRestaurantsToRows(rows, restaurants);
        ReplyKeyboardMarkup keyboard = ku.addBackButtonLast(rows, langISO);
        if (restaurants.size() % 2 == 1) {
            keyboard = ku.concatLastTwoRows(keyboard);
        }
        sendMessage.setReplyMarkup(keyboard.setResizeKeyboard(true));
    }

    private void handleContactUs(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                 ResourceBundle rb) {
        SendMessage contactUsMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("contact-us-message"));
        setContactUsKeyboard(contactUsMessage, telegramUser.getLangISO());
        try {
            bot.execute(contactUsMessage);
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
