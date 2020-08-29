package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookOrderDto {
    @JsonProperty("order_id")
    private String orderId;
    private int number;
    private WebhookOrderStatus status;

    public enum WebhookOrderStatus {
        NEW(0),
        ACCEPTED(1),
        CANCELLED(2),
        SENT(3),
        DELIVERED(4);

        private final int jsonValue;

        WebhookOrderStatus(int jsonValue) {
            this.jsonValue = jsonValue;
        }

        @JsonValue
        public int getJsonValue() {
            return jsonValue;
        }
    }
}
