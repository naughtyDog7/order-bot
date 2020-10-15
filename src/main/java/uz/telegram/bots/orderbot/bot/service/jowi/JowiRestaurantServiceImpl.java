package uz.telegram.bots.orderbot.bot.service.jowi;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uz.telegram.bots.orderbot.bot.dto.DayDto;
import uz.telegram.bots.orderbot.bot.dto.RestaurantDto;
import uz.telegram.bots.orderbot.bot.service.RestaurantService;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.user.TelegramLocation;
import uz.telegram.bots.orderbot.bot.user.WorkingTime;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class JowiRestaurantServiceImpl implements JowiRestaurantService {
    private final RestTemplate restTemplate;
    private final RestaurantService restaurantService;
    private final UriUtil uriUtil;

    /**
     * etag for restaurants from JOWI API
     */
    private String etag = "";

    private static final TypeRef<List<RestaurantDto>> RESTAURANTS_TYPE_REF = new TypeRef<>() {
    };

    JowiRestaurantServiceImpl(RestTemplate restTemplate, RestaurantService restaurantService, UriUtil uriUtil) {
        this.restTemplate = restTemplate;
        this.restaurantService = restaurantService;
        this.uriUtil = uriUtil;
    }

    /**
     * This method fetches all restaurants from JOWI api, and saves them if changed, or not exists in repository
     *
     * @return loaded from database restaurants
     * @throws IllegalStateException if incorrect or invalid response received
     */
    public List<Restaurant> fetchAndUpdateRestaurants() throws IOException {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getRestaurantsGetUri())
                .ifNoneMatch(etag)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();
        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);
        return updateRestaurantsFromResponse(jsonResponse);
    }

    private List<Restaurant> updateRestaurantsFromResponse(ResponseEntity<String> jsonResponse) throws IOException {
        if (jsonResponse.getStatusCodeValue() == HttpStatus.NOT_MODIFIED.value()) {
            return restaurantService.findAll();
        } else if (jsonResponse.getStatusCodeValue() == HttpStatus.OK.value()) {
            etag = jsonResponse.getHeaders().getETag();
            DocumentContext context = JsonPath.parse(jsonResponse.getBody());
            if (context.read("$.status", Integer.class) != 1)
                throw new IOException("Was waiting for status 1 in response, response = " + jsonResponse);

            return updateRestaurantsFromJsonContext(context);
        } else {
            throw new IOException("Was waiting for status code 200 or 304, got response " + jsonResponse);
        }
    }

    private List<Restaurant> updateRestaurantsFromJsonContext(DocumentContext context) {
        List<Restaurant> newRestaurants = new ArrayList<>();
        List<Restaurant> oldRestaurants = restaurantService.findAll();
        List<RestaurantDto> restaurantDtos = context.read("$.restaurants", RESTAURANTS_TYPE_REF);
        for (RestaurantDto dto : restaurantDtos) {
            Restaurant oldRestaurant = oldRestaurants.stream()
                    .filter(r -> r.getRestaurantId().equals(dto.getId()))
                    .findAny().orElse(null);
            Restaurant restaurant = getNewOrUpdateOldRestaurant(dto, oldRestaurant);
            newRestaurants.add(restaurant);
        }
        deleteRestaurantsRemovedFromServer(oldRestaurants, newRestaurants);
        return restaurantService.saveAll(newRestaurants);
    }

    private void deleteRestaurantsRemovedFromServer(List<Restaurant> oldRestaurants, List<Restaurant> newRestaurants) {
        oldRestaurants.stream()
                .filter(rtd -> newRestaurants.stream()
                        .map(Restaurant::getRestaurantId)
                        .noneMatch(id -> rtd.getRestaurantId().equals(id)))
                .forEach(restaurantService::delete);
    }

    public Restaurant getNewOrUpdateOldRestaurant(RestaurantDto restaurantDto, Restaurant oldRestaurant) {
        Map<DayOfWeek, WorkingTime> workingTimes = restaurantDto.getDays().stream()
                .filter(d -> d.getTimetableCode() == 1)
                .collect(Collectors.toMap(d -> DayOfWeek.of(d.getDayCode()), DayDto::toWorkingTime,
                        (f, s) -> s, () -> new EnumMap<>(DayOfWeek.class)));
        TelegramLocation location = TelegramLocation.from(restaurantDto.getLatitude(), restaurantDto.getLongitude());
        Restaurant restaurant;
        restaurant = Objects.requireNonNullElseGet(oldRestaurant, Restaurant::new);
        restaurant.setRestaurantId(restaurantDto.getId());
        restaurant.setRestaurantTitle(restaurantDto.getTitle());
        restaurant.setLocation(location);
        restaurant.setWorkingTime(workingTimes);
        restaurant.setAddress(restaurantDto.getAddress());
        restaurant.setOnlineOrder(restaurantDto.isOnlineOrder());
        restaurant.setDeliveryPrice(restaurantDto.getDeliveryPrice());
        return restaurant;
    }
}
