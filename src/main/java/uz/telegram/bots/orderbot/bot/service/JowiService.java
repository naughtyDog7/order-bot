package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.io.IOException;
import java.util.List;

public interface JowiService {
    List<Category> updateAndFetchNonEmptyCategories(String restaurantId) throws IOException;
    List<Restaurant> updateAndFetchRestaurants() throws IOException;
    void postOrder(Order order, TelegramUser user) throws IOException;
    void cancelOrderOnServer(Order order, String cancellationReason) throws IOException;
    int getOrderStatusValueFromServer(String orderStringId) throws IOException;
}
