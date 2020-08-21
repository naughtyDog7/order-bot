package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.PaymentInfo;

public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {
}
