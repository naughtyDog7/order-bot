package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.user.Location;
import uz.telegram.bots.orderbot.bot.user.Restaurant;

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

    public static Restaurant toRestaurant(RestaurantDto restaurantDto) {
        Location location = Location.of(restaurantDto.longitude, restaurantDto.latitude);
        Restaurant restaurant = new Restaurant(restaurantDto.id, restaurantDto.title, location);
        return restaurant;
    }
}
