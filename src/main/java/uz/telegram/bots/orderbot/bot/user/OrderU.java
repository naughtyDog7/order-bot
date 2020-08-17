package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class OrderU {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_user_id")
    private TelegramUser telegramUser;

    @OneToMany(mappedBy = "orderU", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private final List<Product> products = new ArrayList<>();

    private double finalPrice;

    @OneToOne
    private Location location;

    @OneToOne
    private PaymentInfo paymentInfo;

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
}
