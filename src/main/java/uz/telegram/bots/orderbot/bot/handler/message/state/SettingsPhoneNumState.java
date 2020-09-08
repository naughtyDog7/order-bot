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
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.SETTINGS;

@Component
class SettingsPhoneNumState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final ProductWithCountService pwcService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;

    @Autowired
    SettingsPhoneNumState(ResourceBundleFactory rbf, TelegramUserService userService,
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

    private static final Pattern PHONE_NUM_PATTERN = Pattern.compile("^(?:\\+?998)?[ -]?(\\d{2})[ -]?(\\d{3})[ -]?(\\d{2})[ -]?(\\d{2})$");

    @Override
    //can come as contact, back button, confirm button, and change button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        if (message.hasText()) {
            String btnBack = rb.getString("btn-back");
            String text = message.getText();
            if (text.equals(btnBack))
                handleBack(bot, telegramUser, rb);
            else handlePhoneNum(bot, telegramUser, rb, text);
        } else if (message.hasContact()) {
            Contact contact = message.getContact();
            String phoneNum = contact.getPhoneNumber();
            handlePhoneNum(bot, telegramUser, rb, phoneNum);
        } else {
            DefaultBadRequestHandler.handleContactBadRequest(bot, telegramUser, rb);
        }
    }

    private void handlePhoneNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String phoneNum) {
        Matcher m = PHONE_NUM_PATTERN.matcher(phoneNum);
        String cleanedPhoneNum;
        if (m.matches()) {
            cleanedPhoneNum = m.group(1) + "-" + m.group(2) + "-" + m.group(3) + "-" + m.group(4);
        } else {
            DefaultBadRequestHandler.handleBadPhoneNumber(bot, telegramUser, rb);
            return;
        }
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("phone-set-success"));
        ku.setSettingsKeyboard(sendMessage, rb, telegramUser.getLangISO(), kf, true);
        telegramUser.setPhoneNum(cleanedPhoneNum);
        userService.save(telegramUser);

        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(SETTINGS);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
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
