package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "location")
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class TelegramLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private final double longitude, latitude;

    public static TelegramLocation of(double longitude, double latitude) {
        return new TelegramLocation(longitude, latitude);
    }

}
