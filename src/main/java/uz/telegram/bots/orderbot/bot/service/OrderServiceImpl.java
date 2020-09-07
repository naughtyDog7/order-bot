package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;
import uz.telegram.bots.orderbot.bot.repository.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static uz.telegram.bots.orderbot.bot.user.Order.OrderState.ACTIVE;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {


    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final RestaurantRepository restaurantRepository;
    private final ProductWithCountRepository productWithCountRepository;
    private final PaymentInfoRepository paymentInfoRepository;
    private final LocationRepository locationRepository;
    private final UriUtil uriUtil;
    private final RestTemplate restTemplate;
    private final JowiProperties jowiProperties;

    @Autowired
    public OrderServiceImpl(OrderRepository repository, ProductRepository productRepository,
                            ProductService productService, RestaurantRepository restaurantRepository,
                            ProductWithCountRepository productWithCountRepository,
                            PaymentInfoRepository paymentInfoRepository, LocationRepository locationRepository,
                            UriUtil uriUtil, RestTemplate restTemplate, JowiProperties jowiProperties) {
        this.orderRepository = repository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.restaurantRepository = restaurantRepository;
        this.productWithCountRepository = productWithCountRepository;
        this.paymentInfoRepository = paymentInfoRepository;
        this.locationRepository = locationRepository;
        this.uriUtil = uriUtil;
        this.restTemplate = restTemplate;
        this.jowiProperties = jowiProperties;
    }

    @Override
    public Optional<Order> findActive(TelegramUser user) {
        return orderRepository.findFirstByStateIsAndTelegramUser(ACTIVE, user);
    }

    @Override
    public <S extends Order> S save(S s) {
        return orderRepository.save(s);
    }

    @Override
    public Optional<Order> findByOrderStringId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    @Override
    public void deleteOrder(Order order) {
        List<ProductWithCount> productsWithCount = productWithCountRepository.findByOrderId(order.getId());
        for (ProductWithCount productWithCount : productsWithCount) {
            Product product = productService.fromProductWithCount(productWithCount.getId());
            product.setCountLeft(product.getCountLeft() + productWithCount.getCount());
            productRepository.save(product);
        }
        orderRepository.delete(order);
    }

    @Override
    public List<Order> findActiveForRestaurant(Restaurant restaurant) {
        return orderRepository.findAllByStateAndRestaurantStringId(ACTIVE, restaurant.getRestaurantId());
    }
}
