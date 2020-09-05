package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    Optional<Category> findByNameAndRestaurantId(String name, int restaurantId);

    List<Category> findNonEmptyByRestaurantStringId(String id);

    List<Category> findNonEmptyByOrderId(long orderId);

    Optional<Category> getLastChosenByOrder(Order order);

    List<Category> findAllByRestaurantStringId(String id);

    List<Category> saveAll(List<Category> categories);

    void delete(Category category);
}
