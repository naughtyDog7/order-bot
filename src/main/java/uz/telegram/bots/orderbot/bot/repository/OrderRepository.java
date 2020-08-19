package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.OrderU;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderU, Long> {
    Optional<OrderU> findFirstByStateIsAndTelegramUser(
            OrderU.OrderState state, TelegramUser user); // used with active order state to retrieve current order
}
