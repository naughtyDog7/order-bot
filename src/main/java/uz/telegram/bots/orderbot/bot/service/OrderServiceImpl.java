package uz.telegram.bots.orderbot.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.telegram.bots.orderbot.bot.dto.CancelOrderDto;
import uz.telegram.bots.orderbot.bot.dto.CourseDto;
import uz.telegram.bots.orderbot.bot.dto.OrderDto;
import uz.telegram.bots.orderbot.bot.dto.OrderWrapper;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;
import uz.telegram.bots.orderbot.bot.repository.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uz.telegram.bots.orderbot.bot.dto.OrderDto.OrderType.DELIVERY;

@Service
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
    public void postOrder(Order order, TelegramUser user) throws IOException {
        List<ProductWithCount> products = productWithCountRepository.getWithProductsByOrderId(order.getId());
        Restaurant restaurant = restaurantRepository.getByOrderId(order.getId());
        OrderWrapper orderWrapper = new OrderWrapper(jowiProperties.getApiKey(), jowiProperties.getSig(), restaurant.getRestaurantId());
        PaymentInfo paymentInfo = paymentInfoRepository.getByOrderId(order.getId());
        TelegramLocation location = locationRepository.getByPaymentInfoId(paymentInfo.getId());
        OrderDto orderDto = new OrderDto(location.toString(), null,
                user.getPhoneNum(), DELIVERY, 0, paymentInfo.getPaymentMethod().toDto());
        orderWrapper.setOrder(orderDto);
        orderDto.setCourses(products.stream()
                .map(CourseDto::fromProductWithCount)
                .collect(Collectors.toList()));
        long amountOrder = 0;
        for (ProductWithCount product : products) {
            amountOrder += product.getCount() * (long) product.getProduct().getPrice();
        }
        orderDto.setAmountOrder(amountOrder);

        RequestEntity<OrderWrapper> requestEntity = RequestEntity.post(uriUtil.getOrderPostUri())
                .contentType(APPLICATION_JSON)
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
            throw new IOException("Was waiting for status 1 in response, response = " + jsonResponse);
        }
        String orderId = context.read("$.order.id", String.class);
        order.setOrderId(orderId);
        orderRepository.save(order);
    }

    @Override
    public Optional<Order> getByOrderStringId(String orderId) {
        return orderRepository.getByOrderId(orderId);
    }

    @Override
    public int getOrderStatusValueFromServer(String orderStringId) throws IOException {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getOrderGetUri(orderStringId))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);
        String body = jsonResponse.getBody();
        if (body == null)
            throw new IOException("Server have not responded with order");

        DocumentContext context = JsonPath.parse(body);
        if (context.read("$.status", Integer.class) != 1)
            throw new IOException("Received status other than 1, jsonResponse = " + jsonResponse);

        Integer orderStatus = context.read("$.order.status", Integer.class);
        if (orderStatus == null)
            throw new IOException("Can't find order status, jsonResponse = " + jsonResponse);
        return orderStatus;
    }

    @Override
    public void cancelOrderOnServer(Order order, String cancellationReason) throws IOException {
        CancelOrderDto cancel = new CancelOrderDto(jowiProperties.getApiKey(), jowiProperties.getSig(), cancellationReason);
        RequestEntity<CancelOrderDto> requestEntity = RequestEntity.post(uriUtil.getOrderCancelUri(order.getOrderId()))
                .contentType(APPLICATION_JSON)
                .body(cancel);
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);
        DocumentContext context = JsonPath.parse(responseEntity.getBody());
        if (context.read("$.status", Integer.class) != 1) {
            throw new IOException("Cancellation wasn't proceeded, received status other than 1, jsonResponse = " + responseEntity);
        }

    }
}
