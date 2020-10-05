package uz.telegram.bots.orderbot.bot.service.jowi;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.io.IOException;
import java.util.List;

interface JowiCategoryService {
    List<Category> updateAndFetchNonEmptyCategories(String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser) throws IOException;
}
