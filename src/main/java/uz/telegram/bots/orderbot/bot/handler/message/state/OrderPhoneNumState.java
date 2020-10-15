package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.CategoryService;
import uz.telegram.bots.orderbot.bot.service.OrderService;
import uz.telegram.bots.orderbot.bot.service.ProductWithCountService;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

import static uz.telegram.bots.orderbot.bot.handler.message.state.UserState.LOCATION_SENDING;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.LOCATION_KEYBOARD;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.PHONE_NUM_ENTER_KEYBOARD;

@Component
@Slf4j
class OrderPhoneNumState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final ProductWithCountService pwcService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    OrderPhoneNumState(ResourceBundleFactory rbf, TelegramUserService userService,
                       OrderService orderService, CategoryService categoryService,
                       ProductWithCountService pwcService, KeyboardFactory kf, KeyboardUtil ku,
                       LockFactory lf, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.pwcService = pwcService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as contact, back button, confirm button, and change button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.findActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (message.hasText()) {
                String messageText = message.getText();
                handleMessageText(bot, telegramUser, rb, order, messageText);
            } else if (message.hasContact()) {
                handleContact(bot, telegramUser, message.getContact(), rb);
            } else {
                badRequestHandler.handleContactBadRequest(bot, telegramUser, rb);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleContact(TelegramLongPollingBot bot, TelegramUser telegramUser,
                               Contact contact, ResourceBundle rb) {
        String phoneNum = contact.getPhoneNumber();
        handleNewPhoneNum(bot, telegramUser, rb, phoneNum);
    }

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, String messageText) {
        String btnBack = rb.getString("btn-back");
        String btnConfirm = rb.getString("btn-confirm");
        String btnChangeNum = rb.getString("btn-change-existing-phone-num");
        String phoneNum = telegramUser.getPhoneNum();
        if (messageText.equals(btnBack))
            handleBack(bot, telegramUser, rb, order);
        else if (phoneNum != null && messageText.equals(btnConfirm))
            handleConfirmPhoneNum(bot, telegramUser, rb);
        else if (phoneNum != null && messageText.equals(btnChangeNum))
            handleChangePhoneNum(bot, telegramUser, rb);
        else
            handleNewPhoneNum(bot, telegramUser, rb, messageText);
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        long orderId = order.getId();
        List<Category> categories = categoryService.findNonEmptyByOrderId(orderId);
        int basketNumItems = pwcService.getBasketItemsCount(orderId);
        ToOrderMainHandler.builder()
                .bot(bot)
                .service(userService)
                .rb(rb)
                .telegramUser(telegramUser)
                .ku(ku)
                .kf(kf)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems, ToOrderMainHandler.CallerPlace.OTHER);
    }

    private void handleConfirmPhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage phoneNumConfirmedMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("phone-num-confirmed"));
        try {
            bot.execute(phoneNumConfirmedMessage);
            toLocationChoosing(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void toLocationChoosing(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        try {
            sendLocationMessage(bot, telegramUser, rb);
            telegramUser.setCurState(LOCATION_SENDING);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendLocationMessage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) throws TelegramApiException {
        SendMessage locationRequestMessage = new SendMessage()
                .setText(rb.getString("request-send-location"))
                .setChatId(telegramUser.getChatId());
        setLocationKeyboard(telegramUser, locationRequestMessage);
        bot.execute(locationRequestMessage);
    }

    private void setLocationKeyboard(TelegramUser telegramUser, SendMessage locationRequestMessage) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(LOCATION_KEYBOARD, telegramUser.getLangISO());
        locationRequestMessage.setReplyMarkup(ku.addBackButtonLast(keyboard, telegramUser.getLangISO())
                .setResizeKeyboard(true));
    }

    private void handleChangePhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage phoneNumRequestMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("press-to-send-contact"));
        setPhoneKeyboard(telegramUser, phoneNumRequestMessage);
        try {
            bot.execute(phoneNumRequestMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setPhoneKeyboard(TelegramUser telegramUser, SendMessage message) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(PHONE_NUM_ENTER_KEYBOARD, telegramUser.getLangISO());
        message.setReplyMarkup(ku.addBackButtonLast(keyboard, telegramUser.getLangISO())
                .setResizeKeyboard(true));
    }

    private void handleNewPhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String phoneNum) {
        try {
            userService.checkAndSetPhoneNum(telegramUser, phoneNum);
            log.info("Phone num updated, telegram user " + telegramUser);
            userService.save(telegramUser);
            handlePhoneNumUpdated(bot, telegramUser, rb);
        } catch (IllegalArgumentException e) {
            badRequestHandler.handleBadPhoneNumber(bot, telegramUser, rb);
        }
    }

    private void handlePhoneNumUpdated(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        try {
            sendPhoneNumUpdatedMessage(bot, telegramUser, rb);
            toLocationChoosing(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoneNumUpdatedMessage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) throws TelegramApiException {
        SendMessage phoneSetSuccessMessage = new SendMessage()
            .setText(rb.getString("phone-set-success"))
            .setChatId(telegramUser.getChatId());
        bot.execute(phoneSetSuccessMessage);
    }
}
