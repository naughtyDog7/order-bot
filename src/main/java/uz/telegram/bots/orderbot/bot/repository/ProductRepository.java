package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.telegram.bots.orderbot.bot.user.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> getAllByCategoryId(int categoryId);
    Optional<Product> getByCategoryIdAndName(int categoryId, String name);
    Optional<Product> getByProductId(String stringId);
    @Query(value = "SELECT p FROM ProductWithCount pwc " +
            "JOIN pwc.product p " +
            "WHERE pwc.id = :productWithCountId")
    Product findByProductWithCountId(long productWithCountId);

    List<Product> deleteAllByCategoryId(int categoryId);
}
