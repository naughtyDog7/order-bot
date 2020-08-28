package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.SETTINGS;

@Component
class PhoneNumEnterState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final ProductWithCountService pwcService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;

    @Autowired
    PhoneNumEnterState(ResourceBundleFactory rbf, TelegramUserService userService,
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
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Optional<Order> optOrder = orderService.getActive(telegramUser);
            if (message.hasText()) {
                String btnBack = rb.getString("btn-back");
                String text = message.getText();
                if (text.equals(btnBack))
                    handleBack(bot, telegramUser, rb, optOrder);
                else
                    DefaultBadRequestHandler.handleContactBadRequest(bot, telegramUser, rb);
                return;
            } else if (!message.hasContact()) {
                DefaultBadRequestHandler.handleContactBadRequest(bot, telegramUser, rb);
                return;
            }
            Contact contact = message.getContact();
            String phoneNum = contact.getPhoneNumber();

            handlePhoneNum(bot, telegramUser, rb, optOrder, phoneNum);

        } finally {
            lock.unlock();
        }
    }

    private void handlePhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Optional<Order> optOrder, String phoneNum) {
        if (optOrder.isPresent()) {
            handleInOrder(bot, telegramUser, rb, optOrder.get());
        } else {
            handleInSettings(bot, telegramUser, rb, phoneNum);
        }
    }

    private void handleInSettings(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String phoneNum) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("phone-set-success"));
        telegramUser.setPhoneNum(phoneNum);
        userService.save(telegramUser);

        ku.setSettingsKeyboard(sendMessage, rb, telegramUser.getLangISO(), kf, true);
        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(SETTINGS);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleInOrder(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {

    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Optional<Order> optOrder) {
        if (optOrder.isPresent()) {
            long orderId = optOrder.get().getId();
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
                    .handleToOrderMain(basketNumItems);
        } else {
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
    }
}
