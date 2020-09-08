package uz.telegram.bots.orderbot.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import uz.telegram.bots.orderbot.bot.dto.*;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uz.telegram.bots.orderbot.bot.dto.OrderDto.OrderType.DELIVERY;

@Service
public class JowiServiceImpl implements JowiService {

    private final UriUtil uriUtil;
    private final RestTemplate restTemplate;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final ProductService productService;
    private final RestaurantService restaurantService;
    private final ProductWithCountService pwcService;
    private final PaymentInfoService paymentInfoService;
    private final LocationService locationService;
    private final JowiProperties jowiProperties;

    public JowiServiceImpl(UriUtil uriUtil, RestTemplate restTemplate,
                           CategoryService categoryService, OrderService orderService,
                           ProductService productService, RestaurantService restaurantService,
                           ProductWithCountService pwcService, PaymentInfoService paymentInfoService,
                           LocationService locationService, JowiProperties jowiProperties) {
        this.uriUtil = uriUtil;
        this.restTemplate = restTemplate;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.productService = productService;
        this.restaurantService = restaurantService;
        this.pwcService = pwcService;
        this.paymentInfoService = paymentInfoService;
        this.locationService = locationService;
        this.jowiProperties = jowiProperties;
    }


    // this map is used to save etags
    /**
     * This map is used to save etags for restaurants
     * key -> restaurant id
     * value -> etag
     */
    private final Map<String, String> restaurantIdEtags = new HashMap<>();

    private static final TypeRef<List<CategoryDto>> CATEGORY_DTO_TYPE_REF = new TypeRef<>() {
    };


    /**
     * This method is used to fetch categories for specific restaurant from JOWI api
     *
     * @param restaurantId restaurant id which can be taken from JOWI api
     * @param bot telegram bot used to send message in case of deleting product which was in someone's basket
     * @param telegramUser
     * @return loaded categories
     * @throws IllegalStateException if incorrect or invalid response received
     */
    @Override
    //this method fetches categories from jowi api, if anything changed, saves to repo
    public List<Category> updateAndFetchNonEmptyCategories(String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser) throws IOException {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getMenuGetUri(restaurantId))
                .ifNoneMatch(restaurantIdEtags.getOrDefault(restaurantId, "noetag"))
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();

        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);
        if (jsonResponse.getStatusCodeValue() == HttpStatus.NOT_MODIFIED.value()) {
            return categoryService.findNonEmptyByRestaurantStringId(restaurantId);


        } else if (jsonResponse.getStatusCodeValue() == HttpStatus.OK.value()) {
            restaurantIdEtags.put(restaurantId, jsonResponse.getHeaders().getETag());
            DocumentContext context = JsonPath.parse(jsonResponse.getBody());
            if (context.read("$.status", Integer.class) != 1)
                throw new IOException("Was waiting for status 1 in response, response = " + jsonResponse);

            List<Category> categories = new ArrayList<>();
            List<Category> categoriesToDelete = categoryService.findAllByRestaurantStringId(restaurantId);
            List<CategoryDto> resultFromServer;
            try {
                resultFromServer = context.read("$.categories", CATEGORY_DTO_TYPE_REF);
            } catch (PathNotFoundException e) { //this is if no categories were returned from jowi server
                resultFromServer = new ArrayList<>();
            }
            for (CategoryDto dto : resultFromServer) {
                Category oldCategory = categoriesToDelete.stream().filter(c -> c.getName().equals(dto.getTitle())).findAny().orElse(null);
                Category category = getNewOrUpdateOldCategory(dto, oldCategory, restaurantId, bot, telegramUser);
                categories.add(category);
            }
            categoriesToDelete.stream()
                    .filter(ctd -> categories.stream()
                            .map(Category::getName)
                            .noneMatch(c -> ctd.getName().equals(c)))
                    .forEach(c -> {
                        productService.getAllByCategoryId(c.getId()).forEach(p -> productService.delete(p, restaurantId, bot, telegramUser));
                        categoryService.delete(c);
                    });


            categories.forEach(category -> category.setRestaurant(restaurantService.findByRestaurantId(restaurantId)
                    .orElseThrow(() -> new AssertionError("Restaurant must be found at this point"))));

            List<Category> result = categoryService.saveAll(categories);
            updateProductInformation(result, restaurantId);
            return result;
        } else {
            throw new IOException("Was waiting for status code 200 or 304, got response " + jsonResponse);
        }
    }

    private Category getNewOrUpdateOldCategory(CategoryDto categoryDto, Category oldCategory, String restaurantId,
                                               TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Category category = Objects.requireNonNullElseGet(oldCategory, Category::new);
        List<Product> productsToDelete = productService.getAllByCategoryId(category.getId());
        category.setName(categoryDto.getTitle());
        List<Product> products = new ArrayList<>();
        for (ProductDto productDto : categoryDto.getCourses()) {
            Product product = getNewOrUpdateOldProduct(productDto, productsToDelete.stream()
                    .filter(p -> p.getProductId().equals(productDto.getId()))
                    .findAny().orElse(null));
            product.setCategory(category);
            products.add(product);
        }
        productsToDelete.stream()
                .filter(ptd -> products.stream()
                        .map(Product::getProductId)
                        .noneMatch(id -> ptd.getProductId().equals(id)))
                .forEach(p -> productService.delete(p, restaurantId, bot, telegramUser));
        category.setProducts(products);
        return category;
    }

    public void updateProductInformation(List<? extends Category> categories, String restaurantId) {
        Restaurant restaurant = restaurantService.findByRestaurantId(restaurantId)
                .orElseThrow(() -> new AssertionError("Restaurant must be present at this point"));
        List<Order> activeOrders = orderService.findActiveForRestaurant(restaurant);
        List<Product> products = categories.stream().flatMap(c -> c.getProducts().stream())
                .collect(Collectors.toList());

        products.stream().filter(p -> p.getCountLeft() == -1)
                .forEach(p -> p.setCountLeft(Integer.MAX_VALUE));

        List<ProductWithCount> productWithCounts = activeOrders.stream()
                .flatMap(o -> pwcService.findByOrderId(o.getId()).stream())
                .collect(Collectors.toList());

        for (ProductWithCount pwc : productWithCounts) {
            Product product = productService.fromProductWithCount(pwc.getId());
            int index = products.indexOf(product);
            if (index >= 0) {
                product.setCountLeft(product.getCountLeft() - pwc.getCount());
            }
        }

        productService.saveAll(products);
    }

    public Product getNewOrUpdateOldProduct(ProductDto productDto, Product oldProduct) {
        Product product;
        product = Objects.requireNonNullElseGet(oldProduct, Product::new);
        product.setProductId(productDto.getId());
        product.setName(productDto.getTitle());
        product.setCountLeft((int) Math.round(productDto.getCountLeft()));
        product.setPrice((int) Math.round(productDto.getPrice()));
        product.setImageUrl(productDto.getImageUrl());
        return productService.save(product);
    }

    public Restaurant getNewOrUpdateOldRestaurant(RestaurantDto restaurantDto, Restaurant oldRestaurant) {
        Map<DayOfWeek, WorkingTime> workingTimes = restaurantDto.getDays().stream()
                .filter(d -> d.getTimetableCode() == 1)
                .collect(Collectors.toMap(d -> DayOfWeek.of(d.getDayCode()), DayDto::toWorkingTime,
                        (f, s) -> s, () -> new EnumMap<>(DayOfWeek.class)));
        TelegramLocation location = TelegramLocation.of(restaurantDto.getLatitude(), restaurantDto.getLongitude());
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

    /**
     * etag for restaurants from JOWI API
     */
    private String etag = "";

    private static final TypeRef<List<RestaurantDto>> RESTAURANTS_TYPE_REF = new TypeRef<>() {
    };


    /**
     * This method fetches all restaurants from JOWI api, and saves them if changed, or not exists in repository
     *
     * @return loaded from database restaurants
     * @throws IllegalStateException if incorrect or invalid response received
     */
    public List<Restaurant> updateAndFetchRestaurants() throws IOException {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getRestaurantsGetUri())
                .ifNoneMatch(etag)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();

        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);

        if (jsonResponse.getStatusCodeValue() == HttpStatus.NOT_MODIFIED.value()) {
            return restaurantService.findAll();
        } else if (jsonResponse.getStatusCodeValue() == HttpStatus.OK.value()) {
            etag = jsonResponse.getHeaders().getETag();
            DocumentContext context = JsonPath.parse(jsonResponse.getBody());
            if (context.read("$.status", Integer.class) != 1)
                throw new IOException("Was waiting for status 1 in response, response = " + jsonResponse);

            List<Restaurant> restaurants = new ArrayList<>();
            List<Restaurant> restaurantsToDelete = restaurantService.findAll();
            List<RestaurantDto> restaurantsFromServer = context.read("$.restaurants", RESTAURANTS_TYPE_REF);
            for (RestaurantDto dto : restaurantsFromServer) {
                Restaurant oldRestaurant = restaurantsToDelete.stream()
                        .filter(r -> r.getRestaurantId().equals(dto.getId()))
                        .findAny().orElse(null);
                Restaurant restaurant = getNewOrUpdateOldRestaurant(dto, oldRestaurant);
                restaurants.add(restaurant);
            }
            restaurantsToDelete.stream()
                    .filter(rtd -> restaurants.stream()
                            .map(Restaurant::getRestaurantId)
                            .noneMatch(id -> rtd.getRestaurantId().equals(id)))
                    .forEach(restaurantService::delete);
            return restaurantService.saveAll(restaurants);
        } else {
            throw new IOException("Was waiting for status code 200 or 304, got response " + jsonResponse);
        }
    }

    @Override
    public void postOrder(Order order, TelegramUser user) throws IOException {
        List<ProductWithCount> products = pwcService.findByOrderId(order.getId());
        Restaurant restaurant = restaurantService.findByOrderId(order.getId());
        OrderWrapper orderWrapper = new OrderWrapper(jowiProperties.getApiKey(), jowiProperties.getSig(), restaurant.getRestaurantId());
        PaymentInfo paymentInfo = paymentInfoService.findByOrderId(order.getId());
        TelegramLocation location = locationService.findByPaymentInfoId(paymentInfo.getId());
        OrderDto orderDto = new OrderDto(location.toString(), null,
                user.getPhoneNum(), DELIVERY, 0, paymentInfo.getPaymentMethod().toDto());
        orderWrapper.setOrder(orderDto);
        orderDto.setCourses(products.stream()
                .map(pwc -> CourseDto.fromProductWithCount(pwc, productService))
                .collect(Collectors.toList()));
        long amountOrder = 0;
        for (ProductWithCount product : products) {
            amountOrder += product.getCount() * (long) productService.fromProductWithCount(product.getId()).getPrice();
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
        orderService.save(order);
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
