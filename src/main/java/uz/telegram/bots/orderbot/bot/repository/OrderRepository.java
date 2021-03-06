package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // used with active order state to retrieve current order
    Optional<Order> findFirstByStateIsAndTelegramUser(Order.OrderState state, TelegramUser user);


    @Query(value = "SELECT o FROM Order o "  +
            "JOIN o.paymentInfo pi " +
            "JOIN pi.fromRestaurant r " +
            "WHERE o.state = :state AND r.restaurantId = :restaurantId")
    List<Order> findAllByStateAndRestaurantStringId(Order.OrderState state, String restaurantId);

    Optional<Order> findByOrderId(String orderId);

    @Query("SELECT o FROM ProductWithCount pwc " +
            "JOIN pwc.order o " +
            "WHERE pwc.id = :pwcId")
    Order findByProductWithCountId(long pwcId);

    @Query("SELECT sum(pwc.count * p.price) FROM Order o " +
            "JOIN o.products pwc " +
            "JOIN pwc.product p " +
            "WHERE o.id = :orderId")
    int getProductsPrice(long orderId);
}
