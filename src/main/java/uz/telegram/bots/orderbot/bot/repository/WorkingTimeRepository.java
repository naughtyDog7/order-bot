package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.telegram.bots.orderbot.bot.user.WorkingTime;

public interface WorkingTimeRepository extends JpaRepository<WorkingTime, Integer>, CustomWorkingTimeRepository {
}
