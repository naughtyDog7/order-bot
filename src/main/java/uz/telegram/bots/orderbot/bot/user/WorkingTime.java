package uz.telegram.bots.orderbot.bot.user;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalTime;

@Entity
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class WorkingTime {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private final boolean dayOff;
    private final LocalTime openTime;
    private final LocalTime closeTime;

    public static boolean isWorkingAt(WorkingTime workingTime, LocalTime currentTime) {
        return !workingTime.dayOff && currentTime.isAfter(workingTime.openTime) && currentTime.isBefore(workingTime.closeTime);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isDayOff() {
        return dayOff;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }
}
