package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {
    Optional<Restaurant> findByRestaurantId(String restaurantId);
}
