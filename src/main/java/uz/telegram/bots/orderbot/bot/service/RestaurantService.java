package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.util.List;
import java.util.Optional;

public interface RestaurantService {
    List<Restaurant> updateAndFetchRestaurants();
    Optional<Restaurant> findByRestaurantId(String restaurantId);
}
