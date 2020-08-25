package uz.telegram.bots.orderbot.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.telegram.bots.orderbot.bot.dto.CourseDto;
import uz.telegram.bots.orderbot.bot.dto.OrderDto;
import uz.telegram.bots.orderbot.bot.dto.OrderWrapper;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;
import uz.telegram.bots.orderbot.bot.repository.OrderRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductWithCountRepository;
import uz.telegram.bots.orderbot.bot.repository.RestaurantRepository;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uz.telegram.bots.orderbot.bot.dto.OrderDto.OrderType.DELIVERY;
import static uz.telegram.bots.orderbot.bot.dto.OrderDto.PaymentMethod.ONLINE;

@Service
public class OrderServiceImpl implements OrderService {


    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final RestaurantRepository restaurantRepository;
    private final ProductWithCountRepository productWithCountRepository;
    private final UriUtil uriUtil;
    private final RestTemplate restTemplate;
    private final JowiProperties jowiProperties;

    @Autowired
    public OrderServiceImpl(OrderRepository repository, ProductRepository productRepository,
                            ProductService productService, RestaurantRepository restaurantRepository,
                            ProductWithCountRepository productWithCountRepository,
                            UriUtil uriUtil, RestTemplate restTemplate, JowiProperties jowiProperties) {
        this.orderRepository = repository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.restaurantRepository = restaurantRepository;
        this.productWithCountRepository = productWithCountRepository;
        this.uriUtil = uriUtil;
        this.restTemplate = restTemplate;
        this.jowiProperties = jowiProperties;
    }

    @Override
    public Optional<Order> getActive(TelegramUser user) {
        return orderRepository.findFirstByStateIsAndTelegramUser(Order.OrderState.ACTIVE, user);
    }

    @Override
    public <S extends Order> S save(S s) {
        return orderRepository.save(s);
    }

    @Override
    public void cancelOrder(Order order) {
        List<ProductWithCount> productsWithCount = productWithCountRepository.getAllByOrder(order);
        for (ProductWithCount productWithCount : productsWithCount) {
            Product product = productService.fromProductWithCount(productWithCount.getId());
            product.setCountLeft(product.getCountLeft() + productWithCount.getCount());
            productRepository.save(product);
        }
        orderRepository.delete(order);
    }


    @Override
    public void postOrder(Order order, TelegramUser user) {
        List<ProductWithCount> products = productWithCountRepository.getWithProductsByOrderId(order.getId());
        Restaurant restaurant = restaurantRepository.getByOrderId(order.getId());
        OrderWrapper orderWrapper = new OrderWrapper(jowiProperties.getApiKey(), jowiProperties.getSig(), restaurant.getRestaurantId());
        OrderDto orderDto = new OrderDto("addresssss", "MUZAPPAR",
                "+somenumber", DELIVERY, 0, ONLINE);
        orderWrapper.setOrder(orderDto);
        orderDto.setCourses(products.stream().map(CourseDto::fromProductWithCount).collect(Collectors.toList()));
        orderDto.setAmountOrder(products.stream().map(ProductWithCount::getProduct).mapToInt(Product::getPrice).sum());

        RequestEntity<OrderWrapper> requestEntity = RequestEntity.post(uriUtil.getOrderPostUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderWrapper);

        try {
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(orderWrapper));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);
        String jsonResponse = response.getBody();
        DocumentContext context = JsonPath.parse(jsonResponse);
        if (context.read("$.status", Integer.class) != 1) {
            throw new IllegalStateException("Was waiting for status 1 in response, response = " + jsonResponse);
        }
    }
}
