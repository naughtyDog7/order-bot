package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;

@Component
class OnlinePaymentMessageState implements MessageState {

    private final ResourceBundleFactory rbf;

    @Autowired
    OnlinePaymentMessageState(ResourceBundleFactory rbf) {
        this.rbf = rbf;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        SendMessage payBillMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("please-pay-bill"));
        try {
            bot.execute(payBillMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
