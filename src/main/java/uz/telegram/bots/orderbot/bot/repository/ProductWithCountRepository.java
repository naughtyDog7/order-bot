package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

import java.util.List;
import java.util.Optional;

public interface ProductWithCountRepository extends JpaRepository<ProductWithCount, Long> {
    
    List<ProductWithCount> getAllByOrder(Order order);

    int countAllByOrderId(long orderId);

    Optional<ProductWithCount> findByOrderIdAndProductId(long orderId, long productId);

    List<ProductWithCount> findAllByOrderId(long orderId);

    Optional<ProductWithCount> findByOrderIdAndProductName(long orderId, String productName);

    @Query(value = "SELECT pwc FROM ProductWithCount pwc " +
            "JOIN FETCH pwc.product " +
            "JOIN pwc.order o " +
            "WHERE o.id = :orderId")
    List<ProductWithCount> getWithProductsByOrderId(long orderId);
}
