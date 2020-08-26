package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface RestaurantService {
    List<Restaurant> updateAndFetchRestaurants() throws IOException;
    Optional<Restaurant> findByRestaurantId(String restaurantId);
    Restaurant getByOrderId(long orderId);
}
