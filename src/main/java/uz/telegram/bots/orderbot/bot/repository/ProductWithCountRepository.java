package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

import java.util.List;
import java.util.Optional;

public interface ProductWithCountRepository extends JpaRepository<ProductWithCount, Long> {

    int countAllByOrderId(long orderId);

    Optional<ProductWithCount> findByOrderIdAndProductId(long orderId, long productId);

    List<ProductWithCount> findByOrderId(long orderId);

    Optional<ProductWithCount> findByOrderIdAndProductName(long orderId, String productName);
}
