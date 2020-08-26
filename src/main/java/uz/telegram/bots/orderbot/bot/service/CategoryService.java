package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Category;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> updateAndFetchCategories(String restaurantId) throws IOException;
    Optional<Category> findByNameAndRestaurantId(String name, int restaurantId);
    List<Category> findAll();
    List<Category> findByRestaurantStringId(String id);

    List<Category> findByOrderId(long orderId);
}
