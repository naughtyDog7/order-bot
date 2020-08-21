package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

public interface ProductWithCountRepository extends JpaRepository<ProductWithCount, Long> {

}
