package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    //find by restaurant id in restaurant class
    List<Category> findAllByRestaurantRestaurantId(String restaurantId);
}
