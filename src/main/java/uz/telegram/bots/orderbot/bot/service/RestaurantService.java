package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RestaurantService {
    Optional<Restaurant> findByRestaurantId(String restaurantId);
    Restaurant getByOrderId(long orderId);

    Optional<Restaurant> findByTitle(String restaurantTitle);
    boolean isOpened(LocalDateTime dateTime, Restaurant restaurant);

    List<Restaurant> findAll();

    List<Restaurant> saveAll(List<Restaurant> restaurants);

    void delete(Restaurant restaurant);
}
