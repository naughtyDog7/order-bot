package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.telegram.bots.orderbot.bot.user.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    //find by restaurant id in restaurant class
    List<Category> findAllByRestaurantRestaurantId(String restaurantId);

    Optional<Category> findByNameAndRestaurantId(String name, int restaurantId);

    @Query(value = "SELECT c FROM Order o " +
            "JOIN o.paymentInfo pi " +
            "JOIN pi.fromRestaurant r " +
            "JOIN r.categories c " +
            "WHERE o.id = :orderId")
    List<Category> findByOrderId(long orderId);
}
