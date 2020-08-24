package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Data
@Entity
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(unique = true, nullable = false)
    private final String restaurantId;
    private final String restaurantTitle;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Location location;

    private boolean onlineOrder;
    private double deliveryPrice;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private final List<Category> categories = new ArrayList<>();

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
