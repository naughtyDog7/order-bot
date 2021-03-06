package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.service.jowi.JowiService;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
class FinalConfirmationMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final ProductWithCountService pwcService;
    private final JowiService jowiService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;
    private final RestaurantService restaurantService;
    private final BadRequestHandler badRequestHandler;

    private static final ZoneId TASHKENT_ZONE_ID = ZoneId.of("GMT+5");

    @Autowired
    FinalConfirmationMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                                  CategoryService categoryService, OrderService orderService,
                                  ProductWithCountService pwcService,
                                  JowiService jowiService, KeyboardFactory kf, KeyboardUtil ku, LockFactory lf,
                                  RestaurantService restaurantService, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.pwcService = pwcService;
        this.jowiService = jowiService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
        this.restaurantService = restaurantService;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
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

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                   ResourceBundle rb, String messageText) {
        String btnConfirm = rb.getString("btn-confirm");
        String btnBack = rb.getString("btn-back");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.findActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (messageText.equals(btnBack)) {
                handleBack(bot, telegramUser, rb, order);
            } else if (messageText.equals(btnConfirm)) {
                handleConfirm(bot, telegramUser, rb, order);
            } else {
                badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        List<Category> categories = categoryService.findNonEmptyByOrderId(order.getId());
        int basketNumItems = pwcService.getBasketItemsCount(order.getId());
        ToOrderMainHandler.builder()
                .bot(bot)
                .service(userService)
                .telegramUser(telegramUser)
                .ku(ku)
                .kf(kf)
                .rb(rb)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems, ToOrderMainHandler.CallerPlace.OTHER);
    }

    private void handleConfirm(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        try {
            Restaurant restaurant = restaurantService.findByOrderId(order.getId());
            LocalDateTime curTime = LocalDateTime.now(TASHKENT_ZONE_ID);
            if (!restaurantService.isOpened(curTime, restaurant)) {
                handleRestaurantWasClosed(bot, telegramUser, rb, order);
            } else {
                tryPostOrder(bot, telegramUser, rb, order, curTime);
            }
        } catch (IOException e) {
            log.error("Couldn't send order to server");
            e.printStackTrace();
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleRestaurantWasClosed(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        badRequestHandler.handleRestaurantClosed(bot, telegramUser, rb);
        orderService.deleteOrder(order);
        ToMainMenuHandler.builder()
                .bot(bot)
                .kf(kf)
                .service(userService)
                .telegramUser(telegramUser)
                .rb(rb)
                .build()
                .handleToMainMenu();
    }

    private void tryPostOrder(TelegramLongPollingBot bot, TelegramUser telegramUser,
                              ResourceBundle rb, Order order, LocalDateTime curTime) throws IOException, TelegramApiException {
        jowiService.postOrder(order, telegramUser);
        order.setRequestSendTime(curTime);
        orderService.save(order);
        handleSuccessOrderPost(bot, telegramUser, rb);
    }

    private void handleSuccessOrderPost(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) throws TelegramApiException {
        SendMessage orderWasSentMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("order-was-sent-to-server"));
        setCheckStatusKeyboard(orderWasSentMessage, telegramUser.getLangISO());
        bot.execute(orderWasSentMessage);
        telegramUser.setCurState(UserState.WAITING_ORDER_CONFIRM);
        userService.save(telegramUser);
    }

    private void setCheckStatusKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(KeyboardFactory.KeyboardType.CHECK_STATUS_KEYBOARD, langISO);
        sendMessage.setReplyMarkup(keyboard
                .setResizeKeyboard(true));
    }
}
