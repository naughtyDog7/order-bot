package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Integer> {
    Optional<TelegramUser> findByUserId(int userId);
    List<TelegramUser> findAllByReceiveCommentsTrue();
    @Query("SELECT tu FROM Order o " +
            "JOIN o.telegramUser tu " +
            "WHERE o.id = :orderId")
    TelegramUser getByOrderId(long orderId);
}
