package uz.telegram.bots.orderbot.bot.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;
import java.util.StringJoiner;

@Entity
@Table(name = "location")
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@Getter
@Setter
public class TelegramLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private final double latitude, longitude;

    public static TelegramLocation of(double latitude, double longitude) {
        return new TelegramLocation(latitude, longitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelegramLocation that = (TelegramLocation) o;
        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "[", "]")
                .add("latitude=" + latitude)
                .add("longitude=" + longitude)
                .toString();
    }
}
