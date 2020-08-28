package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.Builder;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;

import java.util.Objects;
import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.PHONE_NUM_ENTER_KEYBOARD;

@Builder
public class ToPhoneNumHandler {
    private final TelegramLongPollingBot bot;
    private final TelegramUser telegramUser;
    private final ResourceBundle rb;
    private final TelegramUserService service;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;

    void handleToPhoneNum(boolean orderChecking) {
        Objects.requireNonNull(bot);
        Objects.requireNonNull(telegramUser);
        Objects.requireNonNull(rb);
        Objects.requireNonNull(service);
        Objects.requireNonNull(kf);
        Objects.requireNonNull(ku);

        if (orderChecking)
            handlePhoneNumInOrder();
        else
            handlePhoneNumInSettings();
    }

    private void handlePhoneNumInSettings() {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("enter-number"));
        setNewPhoneSettingsKeyboard(sendMessage);

        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(TelegramUser.UserState.PHONE_NUM_ENTER);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setNewPhoneSettingsKeyboard(SendMessage sendMessage) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(PHONE_NUM_ENTER_KEYBOARD, telegramUser.getLangISO());
        sendMessage.setReplyMarkup(
                ku.addBackButtonLast(keyboard, telegramUser.getLangISO())
                        .setResizeKeyboard(true));
    }

    private void handlePhoneNumInOrder() {
        System.out.println("this is sparta");
    }
}
