package uz.telegram.bots.orderbot.bot.service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.telegram.bots.orderbot.bot.dto.RestaurantDto;
import uz.telegram.bots.orderbot.bot.repository.RestaurantRepository;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestTemplate restTemplate;
    private final UriUtil uriUtil;


    @Autowired
    public RestaurantServiceImpl(RestaurantRepository restaurantRepository, RestTemplate restTemplate, UriUtil uriUtil) {
        this.restaurantRepository = restaurantRepository;
        this.restTemplate = restTemplate;
        this.uriUtil = uriUtil;
    }

    /**
     * etag for restaurants from JOWI API
     */
    private String etag = "";

    private static final TypeRef<List<RestaurantDto>> RESTAURANTS_TYPE_REF = new TypeRef<>() {};



    /**
     * This method fetches all restaurants from JOWI api, and saves them if changed, or not exists in repository
     * @return loaded from database restaurants
     * @throws IllegalStateException if incorrect or invalid response received
     */
    public List<Restaurant> updateAndFetchRestaurants() {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getRestaurantsGetUri())
                .ifNoneMatch(etag)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();

        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);

        if (jsonResponse.getStatusCodeValue() == HttpStatus.NOT_MODIFIED.value()) {
            return restaurantRepository.findAll();
        } else if (jsonResponse.getStatusCodeValue() == HttpStatus.OK.value()) {
            etag = jsonResponse.getHeaders().getETag();
            DocumentContext context = JsonPath.parse(jsonResponse.getBody());
            if (context.read("$.status", Integer.class) != 1)
                throw new IllegalStateException("Was waiting for status 1 in response, response = " + jsonResponse);

            List<Restaurant> restaurants = context.read("$.restaurants", RESTAURANTS_TYPE_REF)
                    .stream()
                    .map(RestaurantDto::toRestaurant)
                    .collect(Collectors.toList());

            return restaurantRepository.saveAll(restaurants);
        } else {
            throw new IllegalStateException("Was waiting for status code 200 or 304, got response " + jsonResponse);
        }
    }

    public Optional<Restaurant> findByRestaurantId(String restaurantId) {
        return restaurantRepository.findByRestaurantId(restaurantId);
    }
}
