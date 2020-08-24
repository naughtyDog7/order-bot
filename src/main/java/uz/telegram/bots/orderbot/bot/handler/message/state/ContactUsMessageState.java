package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.List;
import java.util.ResourceBundle;

@Component
@Slf4j
class ContactUsMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;

    @Autowired
    ContactUsMessageState(ResourceBundleFactory rbf, TelegramUserService service, KeyboardFactory kf) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            DefaultBadRequestHandler.handleBadRequest(bot, telegramUser, rb);
            return;
        }
        String text = message.getText();

        if (text.equals(rb.getString("btn-back")))
            handleBack(bot, telegramUser, rb);
        else
            handleComment(update, bot, telegramUser, rb);
    }

    private void handleComment(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        List<TelegramUser> commentReceivers = service.getCommentReceivers();
        String commentText = update.getMessage().getText();


        try {
            if (!commentReceivers.isEmpty()) {
                for (TelegramUser receiver : commentReceivers) {
                    SendMessage sendMessage = new SendMessage()
                            .setText("Comment:\n" + update.getMessage().getText())
                            .setChatId(receiver.getChatId())
                            .disableNotification();
                    bot.execute(sendMessage);
                }
            } else {
                log.warn("Received comment \"" + commentText + "\", but no receivers found");
            }

            SendMessage sendMessageToUser = new SendMessage()
                    .setChatId(telegramUser.getChatId())
                    .setText(rb.getString("contact-us-success"));

            bot.execute(sendMessageToUser);
            handleBack(bot, telegramUser, rb);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {
        ToMainMenuHandler.builder()
                .bot(bot)
                .rb(rb)
                .telegramUser(telegramUser)
                .service(service)
                .kf(kf)
                .build()
                .handleToMainMenu();
    }


}
