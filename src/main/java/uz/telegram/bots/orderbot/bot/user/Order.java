package uz.telegram.bots.orderbot.bot.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orderu")
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_user_id")
    private final TelegramUser telegramUser;

    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            fetch = FetchType.LAZY, orphanRemoval = true)
    private final List<ProductWithCount> products = new ArrayList<>();

    private double finalPrice;

    private String lastChosenCategoryName;
    private String lastChosenProductStringId;

    private LocalDateTime requestSendTime;

    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
    private PaymentInfo paymentInfo;

    private OrderState state = OrderState.ACTIVE;

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
        CANCELLED
    }
}
