package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

import java.util.List;
import java.util.Optional;

public interface ProductWithCountRepository extends JpaRepository<ProductWithCount, Long> {

    int countAllByOrderId(long orderId);

    Optional<ProductWithCount> findByOrderIdAndProductId(long orderId, long productId);

    List<ProductWithCount> findByOrderId(long orderId);

    Optional<ProductWithCount> findByOrderIdAndProductName(long orderId, String productName);

    @Query(value = "SELECT pwc FROM ProductWithCount pwc " +
            "JOIN pwc.order o " +
            "JOIN o.paymentInfo pi " +
            "JOIN pi.fromRestaurant r " +
            "JOIN pwc.product p " +
            "WHERE o.state = :orderState AND r.restaurantId = :restaurantId AND p.productId = :productId")
    List<ProductWithCount> findAllToDeleteByRestaurantAndProduct(String restaurantId, String productId, Order.OrderState orderState);
}
