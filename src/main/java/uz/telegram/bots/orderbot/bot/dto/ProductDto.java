package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class ProductDto {
    private String id;
    private String title;
    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("price_for_online_order")
    private double priceForOnlineOrder;

    @JsonProperty("online_order")
    private boolean onlineOrder;

    @JsonProperty("count_left")
    private double countLeft;
}
