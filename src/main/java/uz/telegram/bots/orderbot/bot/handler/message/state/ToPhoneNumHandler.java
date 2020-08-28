package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.Builder;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.PHONE_NUM_ENTER;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.PHONE_NUM_ENTER_KEYBOARD;

@Builder
class ToPhoneNumHandler {
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
                .setText(rb.getString("press-to-send-contact"));
        setNewPhoneKeyboard(sendMessage);

        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(TelegramUser.UserState.PHONE_NUM_ENTER);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setNewPhoneKeyboard(SendMessage sendMessage) {
        ReplyKeyboardMarkup keyboard = kf.getKeyboard(PHONE_NUM_ENTER_KEYBOARD, telegramUser.getLangISO());
        sendMessage.setReplyMarkup(
                ku.addBackButtonLast(keyboard, telegramUser.getLangISO())
                        .setResizeKeyboard(true));
    }

    private void handlePhoneNumInOrder() {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId());
        if (telegramUser.getPhoneNum() == null) {
            sendMessage.setText(rb.getString("start-execute-order-no-phone") + "\n\n" + rb.getString("press-to-send-contact"));
            setNewPhoneKeyboard(sendMessage);
        } else {
            sendMessage.setText(rb.getString("start-execute-order-with-phone")
                            .replace("{phoneNum}", telegramUser.getPhoneNum()));
            setAcceptPhoneOrderKeyboard(sendMessage);
        }
        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(PHONE_NUM_ENTER);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setAcceptPhoneOrderKeyboard(SendMessage sendMessage) {
        KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add(rb.getString("btn-confirm-phone"));
        keyboardButtons.add(rb.getString("btn-change-existing-phone-num"));
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(keyboardButtons);
        sendMessage.setReplyMarkup(ku.addBackButtonLast(rows, telegramUser.getLangISO())
                .setResizeKeyboard(true));
    }
}
