package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Objects;
import java.util.StringJoiner;

@Entity
@Data
@NoArgsConstructor
public class PaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
    private Location orderLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant fromRestaurant;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentInfo that = (PaymentInfo) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }



    @Override
    public String toString() {
        return new StringJoiner(", ", PaymentInfo.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("orderLocation=" + orderLocation)
                .add("fromRestaurant=" + fromRestaurant.getRestaurantTitle())
                .toString();
    }
}
