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
import uz.telegram.bots.orderbot.bot.service.jowi.JowiService;
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
    private final BadRequestHandler badRequestHandler;

    private static final ZoneId TASHKENT_ZONE_ID = ZoneId.of("GMT+5");

    @Autowired
    RestaurantChooseMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                                 RestaurantService restaurantService, CategoryService categoryService,
                                 JowiService jowiService, OrderService orderService, KeyboardFactory kf,
                                 KeyboardUtil ku, LockFactory lf, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.categoryService = categoryService;
        this.jowiService = jowiService;
        this.orderService = orderService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as restaurant name or back button
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
        String btnBack = rb.getString("btn-back");
        if (messageText.equals(btnBack)) {
            handleBack(bot, telegramUser, rb);
        } else {
            handlePotentialRestaurantName(bot, telegramUser, rb, messageText);
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


    private void handlePotentialRestaurantName(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                               ResourceBundle rb, String potentialRestaurantName) {
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Optional<Restaurant> optRestaurant
                    = restaurantService.findByTitle(potentialRestaurantName);
            optRestaurant.ifPresentOrElse(
                    restaurant -> handleRestaurant(bot, telegramUser, rb, restaurant),
                    () -> badRequestHandler.handleTextBadRequest(bot, telegramUser, rb));
        } finally {
            lock.unlock();
        }
    }

    private void handleRestaurant(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Restaurant restaurant) {

        if (!restaurantService.isOpened(LocalDateTime.now(TASHKENT_ZONE_ID), restaurant))
            badRequestHandler.handleRestaurantClosed(bot, telegramUser, rb);
        else
            handleOpenedRestaurant(bot, telegramUser, rb, restaurant);

    }

    private void handleOpenedRestaurant(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Restaurant restaurant) {
        try {
            Message serverLoadingMessage = sendServerLoadingMessage(bot, telegramUser, rb);
            CompletableFuture<List<Category>> future
                    = getCategoriesAsync(bot, telegramUser, rb, restaurant);
            List<Category> categories = future.get(5, TimeUnit.SECONDS);
            if (!categories.isEmpty()) {
                initializeNewOrder(telegramUser, restaurant);
            }
            deleteLoadingMessage(bot, telegramUser, serverLoadingMessage);
            handleToOrderMain(bot, telegramUser, rb, categories);
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

    private CompletableFuture<List<Category>> getCategoriesAsync(TelegramLongPollingBot bot,
                                                                 TelegramUser telegramUser, ResourceBundle rb, Restaurant restaurant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return jowiService.updateAndFetchNonEmptyCategories(restaurant.getRestaurantId(), bot, telegramUser);
            } catch (IOException e) {
                JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
                throw new UncheckedIOException(e);
            }
        });
    }

    private void deleteLoadingMessage(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                      Message sendServerLoadingMessage) throws TelegramApiException {
        DeleteMessage deleteLoadingMessage = new DeleteMessage()
                .setChatId(telegramUser.getChatId())
                .setMessageId(sendServerLoadingMessage.getMessageId());
        bot.execute(deleteLoadingMessage);
    }

    private void initializeNewOrder(TelegramUser telegramUser, Restaurant restaurant) {
        Order order = new Order(telegramUser);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setFromRestaurant(restaurant);
        order.setPaymentInfo(paymentInfo);
        orderService.save(order);
    }

    private void handleToOrderMain(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                   ResourceBundle rb, List<Category> categories) {
        ToOrderMainHandler.builder()
                .bot(bot)
                .telegramUser(telegramUser)
                .service(userService)
                .ku(ku)
                .kf(kf)
                .rb(rb)
                .categories(categories)
                .build()
                .handleToOrderMain(0, ToOrderMainHandler.CallerPlace.MAIN_MENU);
    }
}