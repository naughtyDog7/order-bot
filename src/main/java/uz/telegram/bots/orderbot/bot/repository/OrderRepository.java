package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findFirstByStateIsAndTelegramUser(
            Order.OrderState state, TelegramUser user); // used with active order state to retrieve current order
}
