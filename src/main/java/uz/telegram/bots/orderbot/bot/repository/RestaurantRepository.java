package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {
    Optional<Restaurant> findByRestaurantId(String restaurantId);

    @Query(value = "SELECT r FROM Order o " +
            "JOIN o.paymentInfo pi " +
            "JOIN pi.fromRestaurant r " +
            "WHERE o.id = :orderId AND o.paymentInfo.fromRestaurant.restaurantId = r.restaurantId")
    Restaurant getByOrderId(long orderId);

    Optional<Restaurant> findByRestaurantTitle(String restaurantTitle);
}
