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
import uz.telegram.bots.orderbot.bot.service.*;
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

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.*;

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

    @Autowired
    MainMenuMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                         RestaurantService restaurantService, KeyboardFactory kf, KeyboardUtil ku,
                         JowiService jowiService, OrderService orderService, LockFactory lf, TextUtil tu) {
        this.rbf = rbf;
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.kf = kf;
        this.ku = ku;
        this.jowiService = jowiService;
        this.orderService = orderService;
        this.lf = lf;
        this.tu = tu;
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
            if (orderService.findActive(telegramUser).isPresent()) {
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

            SendMessage loadingMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("server-loading-message"));
            try {
                Message message = bot.execute(loadingMessage);
                CompletableFuture<List<Restaurant>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return jowiService.updateAndFetchRestaurants();
                        // TODO change id to chosen by user restaurant id
                    } catch (IOException e) {
                        JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
                        throw new UncheckedIOException(e);
                    }
                });
                LocalDateTime curTime = LocalDateTime.now(TASHKENT_ZONE_ID);
                List<Restaurant> restaurants = future.get(5, TimeUnit.SECONDS);
                List<Restaurant> workingRestaurants = restaurants
                        .stream()
                        .filter(r -> restaurantService.isOpened(curTime, r))
                        .collect(Collectors.toList());
                DeleteMessage deleteLoadingMessage = new DeleteMessage()
                        .setChatId(telegramUser.getChatId())
                        .setMessageId(message.getMessageId());

                bot.execute(deleteLoadingMessage);

                if (workingRestaurants.size() == 0) {
                    handleNoWorkingRestaurants(bot, telegramUser, rb);
                    return;
                }
                StringBuilder text = new StringBuilder(rb.getString("choose-restaurant")).append("\n\n");
                tu.appendRestaurants(text, restaurants, rb);
                SendMessage sendMessage = new SendMessage()
                        .setChatId(telegramUser.getChatId())
                        .setText(text.toString());
                setRestaurantsKeyboard(sendMessage, workingRestaurants, telegramUser.getLangISO());
                bot.execute(sendMessage);
                telegramUser.setCurState(RESTAURANT_CHOOSE);
                userService.save(telegramUser);
            } catch (TelegramApiException | ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
                log.error("Jowi server timeout");
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleNoWorkingRestaurants(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("no-working-restaurants-for-now"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setRestaurantsKeyboard(SendMessage sendMessage, List<Restaurant> restaurants, String langISO) {
        List<KeyboardRow> rows = new ArrayList<>();
        ku.addRestaurantsToRows(rows, restaurants);
        ReplyKeyboardMarkup keyboard = ku.addBackButtonLast(rows, langISO);
        if (restaurants.size() % 2 == 1) {
            keyboard = ku.concatLastTwoRows(keyboard);
        }
        sendMessage.setReplyMarkup(keyboard.setResizeKeyboard(true));
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
