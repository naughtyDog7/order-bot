package uz.telegram.bots.orderbot.bot.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Location;

import javax.persistence.*;
import java.util.Objects;

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

    public static TelegramLocation from(double latitude, double longitude) {
        return new TelegramLocation(latitude, longitude);
    }

    public static TelegramLocation from(Location location) {
        return new TelegramLocation(location.getLatitude(), location.getLongitude());
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
        return latitude + "," + longitude;
    }
}
