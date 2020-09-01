package uz.telegram.bots.orderbot.bot.handler.message.state;

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

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.LOCATION_SENDING;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.LOCATION_KEYBOARD;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.PHONE_NUM_ENTER_KEYBOARD;

@Component
class OrderPhoneNumState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final ProductWithCountService pwcService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;

    @Autowired
    OrderPhoneNumState(ResourceBundleFactory rbf, TelegramUserService userService,
                       OrderService orderService, CategoryService categoryService,
                       ProductWithCountService pwcService, KeyboardFactory kf, KeyboardUtil ku, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.pwcService = pwcService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
    }

    @Override
    //can come as contact, back button, confirm button, and change button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.getActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (message.hasText()) {
                String btnBack = rb.getString("btn-back");
                String btnConfirm = rb.getString("btn-confirm");
                String btnChangeNum = rb.getString("btn-change-existing-phone-num");
                String text = message.getText();
                if (text.equals(btnBack))
                    handleBack(bot, telegramUser, rb, order);
                else if (text.equals(btnConfirm))
                    handleConfirmPhoneNum(bot, telegramUser, rb);
                else if (text.equals(btnChangeNum))
                    handleChangePhoneNum(bot, telegramUser, rb);
                else
                    DefaultBadRequestHandler.handleContactBadRequest(bot, telegramUser, rb);
                return;
            } else if (!message.hasContact()) {
                DefaultBadRequestHandler.handleContactBadRequest(bot, telegramUser, rb);
                return;
            }
            Contact contact = message.getContact();
            String phoneNum = contact.getPhoneNumber();

            handleNewPhoneNum(bot, telegramUser, rb, phoneNum);

        } finally {
            lock.unlock();
        }
    }

    private void handleConfirmPhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("phone-num-confirmed"));
        try {
            bot.execute(sendMessage);
            toLocationChoosing(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private void handleChangePhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("press-to-send-contact"));

        ReplyKeyboardMarkup keyboard = kf.getKeyboard(PHONE_NUM_ENTER_KEYBOARD, telegramUser.getLangISO());
        sendMessage.setReplyMarkup(ku.addBackButtonLast(keyboard, telegramUser.getLangISO())
                .setResizeKeyboard(true));

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleNewPhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String phoneNum) {
        telegramUser.setPhoneNum(phoneNum);
        userService.save(telegramUser);
        SendMessage sendMessage = new SendMessage()
                .setText(rb.getString("phone-set-success"))
                .setChatId(telegramUser.getChatId());
        try {
            bot.execute(sendMessage);
            toLocationChoosing(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void toLocationChoosing(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage2 = new SendMessage()
                .setText(rb.getString("request-send-location"))
                .setChatId(telegramUser.getChatId());
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(LOCATION_KEYBOARD, telegramUser.getLangISO());
        sendMessage2.setReplyMarkup(ku.addBackButtonLast(keyboard, telegramUser.getLangISO())
                .setResizeKeyboard(true));

        try {
            bot.execute(sendMessage2);
            telegramUser.setCurState(LOCATION_SENDING);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
                .handleToOrderMain(basketNumItems, false);
    }
}
