package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.user.TelegramLocation;
import uz.telegram.bots.orderbot.bot.user.WorkingTime;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @JsonProperty("work_timetable")
    private List<DayDto> days;

    public static Restaurant toRestaurant(RestaurantDto restaurantDto, Restaurant oldRestaurant) {
        Map<DayOfWeek, WorkingTime> workingTimes = restaurantDto.days.stream()
                .filter(d -> d.getTimetableCode() == 1)
                .collect(Collectors.toMap(d -> DayOfWeek.of(d.getDayCode()), DayDto::toWorkingTime,
                        (f, s) -> s, () -> new EnumMap<>(DayOfWeek.class)));
        TelegramLocation location = TelegramLocation.of(restaurantDto.latitude, restaurantDto.longitude);
        Restaurant restaurant = new Restaurant(restaurantDto.id, restaurantDto.title, location, workingTimes);
        restaurant.setOnlineOrder(restaurantDto.onlineOrder);
        restaurant.setDeliveryPrice(restaurantDto.deliveryPrice);
        if (oldRestaurant != null) {
            restaurant.setId(oldRestaurant.getId());
        }
        return restaurant;
    }
}
