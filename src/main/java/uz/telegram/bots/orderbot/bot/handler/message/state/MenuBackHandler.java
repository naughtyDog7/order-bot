package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.Builder;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;

import java.util.Objects;
import java.util.ResourceBundle;

import static uz.telegram.bots.orderbot.bot.user.TelegramUser.UserState.MAIN_MENU;
import static uz.telegram.bots.orderbot.bot.util.KeyboardFactory.KeyboardType.MAIN_MENU_KEYBOARD;

@Builder
public class MenuBackHandler {
    private final TelegramLongPollingBot bot;
    private final TelegramUser telegramUser;
    private final ResourceBundle rb;
    private final TelegramUserService service;
    private final KeyboardFactory kf;

    void handleBackToMenu() {
        Objects.requireNonNull(bot);
        Objects.requireNonNull(telegramUser);
        Objects.requireNonNull(rb);
        Objects.requireNonNull(service);
        Objects.requireNonNull(kf);

        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("main-menu-message"));

        setMenuKeyboard(sendMessage, telegramUser.getLangISO());

        try {
            bot.execute(sendMessage);
            telegramUser.setCurState(MAIN_MENU);
            service.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setMenuKeyboard(SendMessage sendMessage, String langISO) {
        sendMessage.setReplyMarkup(kf.getKeyboard(MAIN_MENU_KEYBOARD, langISO)
                .setResizeKeyboard(true));
    }
}
