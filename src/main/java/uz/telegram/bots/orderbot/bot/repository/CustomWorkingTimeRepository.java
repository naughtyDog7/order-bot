package uz.telegram.bots.orderbot.bot.repository;

import uz.telegram.bots.orderbot.bot.user.WorkingTime;

import java.time.DayOfWeek;
import java.util.Map;

public interface CustomWorkingTimeRepository {
    Map<DayOfWeek, WorkingTime> getWorkingTimes(int restaurantId);
}
