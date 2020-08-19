package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.service.CategoryService;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.List;
import java.util.ResourceBundle;

@Component
class OrderMainMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final KeyboardFactory kf;
    private final CategoryService categoryService;

    @Autowired
    OrderMainMessageState(ResourceBundleFactory rbf, TelegramUserService service, KeyboardFactory kf, CategoryService categoryService) {
        this.rbf = rbf;
        this.service = service;
        this.kf = kf;
        this.categoryService = categoryService;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        String text = message.getText();

        if (text.equals(rb.getString("btn-order-main"))) {
            handleOrder(bot, telegramUser, rb);
            return;
        } else if (text.equals(rb.getString("btn-cancel-order"))) {
            handleCancel(bot, telegramUser, rb);
            return;
        }

        List<Category> categories = categoryService.fetchCategories("test-restaurant-id");
        int index = Category.getNames(categories).indexOf(text);
        if (index != -1)
            handleCategory(bot, telegramUser, rb,
                    categories.get(index));

    }

    private void handleCategory(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Category category) {

    }

    private void handleCancel(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {

    }

    private void handleOrder(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb) {

    }


}
