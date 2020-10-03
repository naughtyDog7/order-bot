package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderWrapper {
    @JsonProperty("api_key")
    private String apiKey;
    private String sig;
    @JsonProperty("restaurant_id")
    private String restaurantId;
    private OrderDto order;

    public OrderWrapper(String apiKey, String sig, String restaurantId) {
        this.apiKey = apiKey;
        this.sig = sig;
        this.restaurantId = restaurantId;
    }
}
