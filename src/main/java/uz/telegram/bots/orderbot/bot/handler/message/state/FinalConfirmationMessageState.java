package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
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

    @Autowired
    FinalConfirmationMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                                  CategoryService categoryService, OrderService orderService,
                                  ProductWithCountService pwcService,
                                  JowiService jowiService, KeyboardFactory kf, KeyboardUtil ku, LockFactory lf, RestaurantService restaurantService) {
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
        String btnConfirm = rb.getString("btn-confirm");
        String btnBack = rb.getString("btn-back");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.getActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (text.equals(btnBack)) {
                handleBack(bot, telegramUser, rb, order);
            } else if (text.equals(btnConfirm)) {
                handleConfirm(bot, telegramUser, rb, order);
            } else {
                DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            }
        } finally {
            lock.unlock();
        }
    }

    private static final ZoneId TASHKENT_ZONE_ID = ZoneId.of("GMT+5");

    private void handleConfirm(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        try {
            Restaurant restaurant = restaurantService.getByOrderId(order.getId());
            LocalDateTime curTime = LocalDateTime.now(TASHKENT_ZONE_ID);
            if (!restaurantService.isOpened(curTime, restaurant)) {
                DefaultBadRequestHandler.handleRestaurantClosed(bot, telegramUser, rb);
                orderService.deleteOrder(order);
                ToMainMenuHandler.builder()
                        .bot(bot)
                        .kf(kf)
                        .service(userService)
                        .telegramUser(telegramUser)
                        .rb(rb)
                        .build()
                        .handleToMainMenu();
            } else {
                jowiService.postOrder(order, telegramUser);
                order.setRequestSendTime(curTime);
                orderService.save(order);

                SendMessage sendMessage = new SendMessage()
                        .setChatId(telegramUser.getChatId())
                        .setText(rb.getString("order-was-sent-to-server"));
                setCheckStatusKeyboard(sendMessage, telegramUser.getLangISO());
                bot.execute(sendMessage);
                telegramUser.setCurState(TelegramUser.UserState.WAITING_ORDER_CONFIRM);
                userService.save(telegramUser);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JowiServerFailureHandler.handleServerFail(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setCheckStatusKeyboard(SendMessage sendMessage, String langISO) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(KeyboardFactory.KeyboardType.CHECK_STATUS_KEYBOARD, langISO);
        sendMessage.setReplyMarkup(keyboard
                .setResizeKeyboard(true));
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
                .handleToOrderMain(basketNumItems, false);
    }
}
