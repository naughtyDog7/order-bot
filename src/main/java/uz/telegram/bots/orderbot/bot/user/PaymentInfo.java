package uz.telegram.bots.orderbot.bot.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.dto.OrderDto;

import javax.persistence.*;
import java.util.StringJoiner;

import static uz.telegram.bots.orderbot.bot.dto.OrderDto.PaymentMethodDto.AFTER_DELIVERY;
import static uz.telegram.bots.orderbot.bot.dto.OrderDto.PaymentMethodDto.ONLINE;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class PaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @OneToOne(cascade = {CascadeType.ALL})
    private TelegramLocation orderLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant fromRestaurant;

    private PaymentMethod paymentMethod;

    @Override
    public String toString() {
        return new StringJoiner(", ", PaymentInfo.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("orderLocation=" + orderLocation)
                .add("fromRestaurant=" + fromRestaurant.getRestaurantTitle())
                .toString();
    }

    public enum PaymentMethod {
        CASH("btn-cash", AFTER_DELIVERY),
        CLICK("btn-click", ONLINE),
        PAYME("btn-payme", ONLINE);

        private final String rbValue;
        private final OrderDto.PaymentMethodDto paymentMethodDto;

        PaymentMethod(String rbValue, OrderDto.PaymentMethodDto paymentMethodDto) {
            this.rbValue = rbValue;
            this.paymentMethodDto = paymentMethodDto;
        }

        public String getRbValue() {
            return rbValue;
        }

        public OrderDto.PaymentMethodDto toDto() {
            return paymentMethodDto;
        }
    }
}
