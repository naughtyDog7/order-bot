package uz.telegram.bots.orderbot.bot.service.jowi;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import uz.telegram.bots.orderbot.bot.dto.*;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uz.telegram.bots.orderbot.bot.dto.OrderDto.OrderType.DELIVERY;

@Service
@Slf4j
class JowiServiceImpl implements JowiService {

    private final UriUtil uriUtil;
    private final RestTemplate restTemplate;
    private final OrderService orderService;
    private final ProductService productService;
    private final RestaurantService restaurantService;
    private final ProductWithCountService pwcService;
    private final PaymentInfoService paymentInfoService;
    private final LocationService locationService;
    private final JowiProperties jowiProperties;
    private final JowiRestaurantService jowiRestaurantService;
    private final JowiCategoryService jowiCategoryService;

    public JowiServiceImpl(UriUtil uriUtil, RestTemplate restTemplate,
                           CategoryService categoryService, OrderService orderService,
                           ProductService productService, RestaurantService restaurantService,
                           ProductWithCountService pwcService, PaymentInfoService paymentInfoService,
                           LocationService locationService, JowiProperties jowiProperties) {
        this.uriUtil = uriUtil;
        this.restTemplate = restTemplate;
        this.orderService = orderService;
        this.productService = productService;
        this.restaurantService = restaurantService;
        this.pwcService = pwcService;
        this.paymentInfoService = paymentInfoService;
        this.locationService = locationService;
        this.jowiProperties = jowiProperties;
        this.jowiRestaurantService = new JowiRestaurantServiceImpl(restTemplate, restaurantService, uriUtil);
        JowiProductService jowiProductService = new JowiProductServiceImpl(productService, restaurantService, orderService, pwcService);
        this.jowiCategoryService = new JowiCategoryServiceImpl(uriUtil, restTemplate, restaurantService, categoryService, productService, jowiProductService);
    }

    @Override
    public List<Restaurant> fetchAndUpdateRestaurants() throws IOException {
        return jowiRestaurantService.fetchAndUpdateRestaurants();
    }

    @Override
    public List<Category> updateAndFetchNonEmptyCategories(String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser) throws IOException {
        return jowiCategoryService.updateAndFetchNonEmptyCategories(restaurantId, bot, telegramUser);
    }

    @Override
    public void postOrder(Order order, TelegramUser user) throws IOException {
        OrderWrapper orderWrapper = prepareOrderWrapperForOrder(order, user);
        RequestEntity<OrderWrapper> requestEntity = RequestEntity.post(uriUtil.getOrderPostUri())
                .contentType(APPLICATION_JSON)
                .body(orderWrapper);

        log.info("Attempting to send order to server: " + orderWrapper.getOrder());
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);
        processPostOrderResponse(response, order);
    }

    private void processPostOrderResponse(ResponseEntity<String> response, Order order) throws IOException {
        String jsonResponse = response.getBody();
        DocumentContext context = JsonPath.parse(jsonResponse);
        if (context.read("$.status", Integer.class) != 1) {
            throw new IOException("Was waiting for status 1 in response, response = " + jsonResponse);
        }
        String orderId = context.read("$.order.id", String.class);
        order.setOrderId(orderId);
        orderService.save(order);
    }

    private OrderWrapper prepareOrderWrapperForOrder(Order order, TelegramUser user) {
        List<ProductWithCount> products = pwcService.findByOrderId(order.getId());
        Restaurant restaurant = restaurantService.findByOrderId(order.getId());
        OrderWrapper orderWrapper = new OrderWrapper(jowiProperties, restaurant.getRestaurantId());
        PaymentInfo paymentInfo = paymentInfoService.findByOrderId(order.getId());
        TelegramLocation location = locationService.findByPaymentInfoId(paymentInfo.getId());
        OrderDto orderDto = new OrderDto(location.toString(), null,
                user.getPhoneNum(), DELIVERY, 0, paymentInfo.getPaymentMethod().toDto());
        orderWrapper.setOrder(orderDto);
        orderDto.setCourses(products.stream()
                .map(pwc -> CourseDto.fromProductWithCount(pwc, productService))
                .collect(Collectors.toList()));
        long amountOrder = orderService.getProductsPrice(order.getId()) + order.getDeliveryPrice();
        orderDto.setDeliveryPrice(order.getDeliveryPrice());
        orderDto.setAmountOrder(amountOrder);
        return orderWrapper;
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
        orderService.deleteOrder(order);
    }

    @Override
    public int getOrderStatusValueFromServer(String orderStringId) throws IOException {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getOrderGetUri(orderStringId))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);
        return getOrderStatusValueFromResponse(jsonResponse);
    }

    private int getOrderStatusValueFromResponse(ResponseEntity<String> jsonResponse) throws IOException {
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
}
