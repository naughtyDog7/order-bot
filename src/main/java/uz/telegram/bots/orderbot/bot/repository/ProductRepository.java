package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> getAllByCategory(Category category);
}
