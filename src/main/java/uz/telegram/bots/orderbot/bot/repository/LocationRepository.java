package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.telegram.bots.orderbot.bot.user.TelegramLocation;

public interface LocationRepository extends JpaRepository<TelegramLocation, Long> {
    @Query("SELECT l FROM PaymentInfo pi " +
            "JOIN pi.orderLocation l " +
            "WHERE pi.id = :id")
    TelegramLocation findByPaymentInfoId(long id);
}
