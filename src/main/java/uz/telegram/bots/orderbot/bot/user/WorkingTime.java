package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class WorkingTime {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private final boolean dayOff;
    private final LocalTime openTime;
    private final LocalTime closeTime;
}
