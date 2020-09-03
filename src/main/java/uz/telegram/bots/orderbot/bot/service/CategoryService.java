package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> updateAndFetchNonEmptyCategories(String restaurantId) throws IOException;
    Optional<Category> findByNameAndRestaurantId(String name, int restaurantId);

    List<Category> findNonEmptyByRestaurantStringId(String id);

    List<Category> findNonEmptyByOrderId(long orderId);

    Optional<Category> getLastChosenByOrder(Order order);
}
