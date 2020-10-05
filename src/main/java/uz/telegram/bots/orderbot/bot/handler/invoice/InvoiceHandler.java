package uz.telegram.bots.orderbot.bot.handler.invoice;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.handler.Handler;
import uz.telegram.bots.orderbot.bot.handler.message.state.ToMainMenuHandler;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
public class InvoiceHandler implements Handler {

    @Setter
    private TelegramLongPollingBot bot;

    private final TelegramUserService service;
    private final LockFactory rlf;
    private final ResourceBundleFactory rbf;
    private final KeyboardFactory kf;

    @Autowired
    public InvoiceHandler(TelegramUserService service, LockFactory rlf, ResourceBundleFactory rbf, KeyboardFactory kf) {
        this.service = service;
        this.rlf = rlf;
        this.rbf = rbf;
        this.kf = kf;
    }

    @Override
    public void handle(Update update) {
        TelegramUser telegramUser = service.getOrSaveAndGetUserFromUpdate(update);
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        Lock lock = rlf.getLockForChatId(telegramUser.getChatId());
        try {
            lock.lock();
            PreCheckoutQuery pcq = update.getPreCheckoutQuery();
            SendMessage sendMessage = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("payment-success"));
            bot.execute(sendMessage);
            ToMainMenuHandler.builder()
                    .kf(kf)
                    .rb(rb)
                    .bot(bot)
                    .service(service)
                    .telegramUser(telegramUser)
                    .build()
                    .handleToMainMenu();
            log.info("Proceeded pre-checkout-query: " + pcq);
            bot.execute(new AnswerPreCheckoutQuery()
                    .setOk(true)
                    .setPreCheckoutQueryId(pcq.getId()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }
}
