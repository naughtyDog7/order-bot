package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class OrderDto {
    private String address;
    private String contact;
    private String phone;
    @JsonProperty("order_type")
    private OrderType orderType;
    private List<CourseDto> courses = new ArrayList<>();
    @JsonProperty("delivery_price")
    private int deliveryPrice;

    @JsonProperty("amount_order")
    private double amountOrder;
    private PaymentMethodDto paymentMethod;

    public OrderDto(String address, String contact, String phone, OrderType orderType, int amountOrder, PaymentMethodDto paymentMethodDto) {
        this.address = address;
        this.contact = contact;
        this.phone = phone;
        this.orderType = orderType;
        this.amountOrder = amountOrder;
        this.paymentMethod = paymentMethodDto;
    }

    public enum OrderType {
        DELIVERY(0),
        PICKUP(1);

        private final int intJsonValue;

        OrderType(int intJsonValue) {
            this.intJsonValue = intJsonValue;
        }

        @JsonValue
        public int getIntJsonValue() {
            return intJsonValue;
        }
    }

    public enum PaymentMethodDto {
        AFTER_DELIVERY(0),
        ONLINE(1);

        private final int intJsonValue;

        PaymentMethodDto(int intJsonValue) {
            this.intJsonValue = intJsonValue;
        }

        @JsonValue
        public int getIntJsonValue() {
            return intJsonValue;
        }
    }
}
