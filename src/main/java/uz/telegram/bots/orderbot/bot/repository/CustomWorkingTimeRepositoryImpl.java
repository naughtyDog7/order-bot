package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uz.telegram.bots.orderbot.bot.user.WorkingTime;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class CustomWorkingTimeRepositoryImpl implements CustomWorkingTimeRepository {
    private final EntityManager em;

    @Autowired
    public CustomWorkingTimeRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Map<DayOfWeek, WorkingTime> getWorkingTimes(int restaurantId) {
        return em.createQuery("SELECT key(wt) as key, value(wt) as value FROM Restaurant r " +
                "JOIN r.workingTime wt WHERE r.id = :restaurantId", Tuple.class).setParameter("restaurantId", restaurantId)
                .getResultStream()
                .collect(Collectors.toMap(t -> t.get("key", DayOfWeek.class), t -> t.get("value", WorkingTime.class),
                        (i, j) -> j, () -> new EnumMap<>(DayOfWeek.class)));
    }
}
