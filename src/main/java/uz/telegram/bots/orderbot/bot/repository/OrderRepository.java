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


    @Query(value = "SELECT o FROM Order o " +
            "JOIN FETCH o.products " +
            "WHERE o.state = :state")
    List<Order> findAllByStateWithProducts(Order.OrderState state);

    Optional<Order> getByOrderId(String orderId);
}
