package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
