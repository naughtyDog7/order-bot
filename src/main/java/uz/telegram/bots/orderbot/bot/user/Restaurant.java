package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.DayOfWeek;
import java.util.*;

import static javax.persistence.FetchType.LAZY;

@Data
@Entity
@NoArgsConstructor(force = true)
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(unique = true, nullable = false)
    private String restaurantId;
    @Column(unique = true, nullable = false)
    private String restaurantTitle;

    @OneToOne(cascade = CascadeType.ALL, fetch = LAZY, orphanRemoval = true)
    private TelegramLocation location;

    private boolean onlineOrder;
    private double deliveryPrice;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = LAZY, orphanRemoval = true)
    private List<Category> categories = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = LAZY, orphanRemoval = true)
    @MapKeyEnumerated
    private Map<DayOfWeek, WorkingTime> workingTime;

    private String address;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Restaurant that = (Restaurant) o;
        return id == that.id &&
                Objects.equals(restaurantId, that.restaurantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, restaurantId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Restaurant.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("restaurantId='" + restaurantId + "'")
                .add("restaurantTitle='" + restaurantTitle + "'")
                .add("location=" + location)
                .add("onlineOrder=" + onlineOrder)
                .add("deliveryPrice=" + deliveryPrice)
                .add("categories=" + Category.getNames(categories))
                .toString();
    }
}
