package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class RestaurantDto {

    private String id;
    private String title;
    private double longitude;
    private double latitude;
    @JsonProperty("online_order")
    private boolean onlineOrder;

    @JsonProperty("delivery_price")
    private double deliveryPrice;

    private String address;

    @JsonProperty("work_timetable")
    private List<DayDto> days;
}
