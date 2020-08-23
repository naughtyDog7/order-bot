package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orderu")
@Data
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_user_id")
    private final TelegramUser telegramUser;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final List<ProductWithCount> products = new ArrayList<>();

    private double finalPrice;

    private String chosenCategoryName = "";
    private String chosenProductStringId = "";

    @OneToOne
    private PaymentInfo paymentInfo;

    private OrderState state = OrderState.ACTIVE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id == order.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserOrder{" +
                "id=" + id +
                ", products=" + products +
                ", finalPrice=" + finalPrice +
                ", paymentInfo=" + paymentInfo +
                '}';
    }

    public enum OrderState {
        ACTIVE,
        ORDERED,
        CANCELLED
    }
}
