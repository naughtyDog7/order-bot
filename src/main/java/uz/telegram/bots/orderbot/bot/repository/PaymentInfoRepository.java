package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.telegram.bots.orderbot.bot.user.PaymentInfo;

public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {
    @Query("SELECT pi FROM Order o " +
            "JOIN o.paymentInfo pi " +
            "WHERE o.id = :orderId")
    PaymentInfo getByOrderAndId(long orderId);
}
