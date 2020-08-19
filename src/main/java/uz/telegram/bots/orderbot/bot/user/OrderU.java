package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class OrderU {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_user_id")
    private final TelegramUser telegramUser;

    @OneToMany(mappedBy = "orderU", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private final List<Product> products = new ArrayList<>();

    private double finalPrice;

    @OneToOne
    private Location location;

    @OneToOne
    private PaymentInfo paymentInfo;

    private OrderState state = OrderState.ACTIVE;

    @Override
    public String toString() {
        return "UserOrder{" +
                "id=" + id +
                ", products=" + products +
                ", finalPrice=" + finalPrice +
                ", location=" + location +
                ", paymentInfo=" + paymentInfo +
                '}';
    }

    public enum OrderState {
        ACTIVE,
        ORDERED,
        CANCELLED
    }
}
