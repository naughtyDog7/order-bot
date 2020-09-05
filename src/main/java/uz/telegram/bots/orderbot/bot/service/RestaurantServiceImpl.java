package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.telegram.bots.orderbot.bot.repository.LocationRepository;
import uz.telegram.bots.orderbot.bot.repository.RestaurantRepository;
import uz.telegram.bots.orderbot.bot.repository.WorkingTimeRepository;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.user.WorkingTime;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import javax.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestTemplate restTemplate;
    private final UriUtil uriUtil;
    private final WorkingTimeRepository workingTimeRepository;
    private final LocationRepository locationRepository;


    @Autowired
    public RestaurantServiceImpl(RestaurantRepository restaurantRepository, RestTemplate restTemplate,
                                 UriUtil uriUtil, WorkingTimeRepository workingTimeRepository, LocationRepository locationRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restTemplate = restTemplate;
        this.uriUtil = uriUtil;
        this.workingTimeRepository = workingTimeRepository;
        this.locationRepository = locationRepository;
    }


    @Override
    public Optional<Restaurant> findByTitle(String restaurantTitle) {
        return restaurantRepository.findByRestaurantTitle(restaurantTitle);
    }

    public Optional<Restaurant> findByRestaurantId(String restaurantId) {
        return restaurantRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public Restaurant getByOrderId(long orderId) {
        return restaurantRepository.getByOrderId(orderId);
    }

    @Override
    public boolean isOpened(LocalDateTime dateTime, Restaurant restaurant) {
        Map<DayOfWeek, WorkingTime> workingTimes = workingTimeRepository.getWorkingTimes(restaurant.getId());
        return WorkingTime.isWorkingAt(workingTimes.get(dateTime.getDayOfWeek()), dateTime.toLocalTime());
    }

    @Override
    public List<Restaurant> findAll() {
        return restaurantRepository.findAll();
    }

    @Override
    public List<Restaurant> saveAll(List<Restaurant> restaurants) {
        return restaurantRepository.saveAll(restaurants);
    }

    @Override
    public void delete(Restaurant restaurant) {
        restaurantRepository.delete(restaurant);
    }
}
