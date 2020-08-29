package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.StringJoiner;

@Data
@Entity
@Table(name = "location")
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class TelegramLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private final double latitude, longitude;

    public static TelegramLocation of(double latitude, double longitude) {
        return new TelegramLocation(latitude, longitude);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "[", "]")
                .add("latitude=" + latitude)
                .add("longitude=" + longitude)
                .toString();
    }
}
