package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
