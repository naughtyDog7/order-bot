package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
class RestaurantChooseMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final RestaurantService restaurantService;
    private final CategoryService categoryService;
    private final JowiService jowiService;
    private final OrderService orderService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;

    @Autowired
    RestaurantChooseMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                                 RestaurantService restaurantService, CategoryService categoryService,
                                 JowiService jowiService, OrderService orderService, KeyboardFactory kf, KeyboardUtil ku, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.categoryService = categoryService;
        this.jowiService = jowiService;
        this.orderService = orderService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }
        String text = message.getText();
        String btnBack = rb.getString("btn-back");

        if (text.equals(btnBack)) {
            handleBack(bot, telegramUser, rb);
        } else {
            Lock lock = lf.getResourceLock();
            try {
                lock.lock();
                Optional<Restaurant> optRestaurant = restaurantService.findByTitle(text);
                if (optRestaurant.isEmpty()) {
                    DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
                } else {
                    handleRestaurant(bot, telegramUser, rb, optRestaurant.get());
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        ToMainMenuHandler.builder()
                .bot(bot)
                .service(userService)
                .telegramUser(telegramUser)
                .kf(kf)
                .rb(rb)
                .build()
                .handleToMainMenu();
    }

    private static final ZoneId TASHKENT_ZONE_ID = ZoneId.of("GMT+5");

    private void handleRestaurant(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Restaurant restaurant) {

        if (!restaurantService.isOpened(LocalDateTime.now(TASHKENT_ZONE_ID), restaurant)) {
            DefaultBadRequestHandler.handleRestaurantClosed(bot, telegramUser, rb);
            return;
        }
        SendMessage loadingMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("server-loading-message"));
        try {
            Message message = bot.execute(loadingMessage);
            CompletableFuture<List<Category>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return jowiService.updateAndFetchNonEmptyCategories(restaurant.getRestaurantId());
                } catch (IOException e) {
                    JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
                    throw new UncheckedIOException(e);
                }
            });
            List<Category> categories = future.get(5, TimeUnit.SECONDS);

            if (!categories.isEmpty()) {
                Order order = new Order(telegramUser);
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setFromRestaurant(restaurant);
                order.setPaymentInfo(paymentInfo);
                orderService.save(order);
            }

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
    }

}