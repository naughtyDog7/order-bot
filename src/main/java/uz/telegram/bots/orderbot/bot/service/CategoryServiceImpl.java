package uz.telegram.bots.orderbot.bot.service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.telegram.bots.orderbot.bot.dto.CategoryDto;
import uz.telegram.bots.orderbot.bot.repository.CategoryRepository;
import uz.telegram.bots.orderbot.bot.repository.OrderRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.repository.RestaurantRepository;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uz.telegram.bots.orderbot.bot.user.Order.OrderState.ACTIVE;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final UriUtil uriUtil;

    @Override
    public List<Category> findByOrderId(long orderId) {
        return categoryRepository.findByOrderId(orderId);
    }

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository, RestaurantRepository restaurantRepository,
                               ProductRepository productRepository, OrderRepository orderRepository, RestTemplate restTemplate, UriUtil uriUtil) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.uriUtil = uriUtil;
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
     * @return loaded categories
     * @throws IllegalStateException if incorrect or invalid response received
     */
    @Override
    //this method fetches categories from jowi api, if anything changed, saves to repo
    public List<Category> updateAndFetchCategories(String restaurantId) {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getMenuGetUri(restaurantId))
                .ifNoneMatch(restaurantIdEtags.getOrDefault(restaurantId, "noetag"))
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();

        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);
        if (jsonResponse.getStatusCodeValue() == HttpStatus.NOT_MODIFIED.value()) {
            return categoryRepository.findAllByRestaurantRestaurantId(restaurantId);


        } else if (jsonResponse.getStatusCodeValue() == HttpStatus.OK.value()) {
            restaurantIdEtags.put(restaurantId, jsonResponse.getHeaders().getETag());
            DocumentContext context = JsonPath.parse(jsonResponse.getBody());
            if (context.read("$.status", Integer.class) != 1)
                throw new IllegalStateException("Was waiting for status 1 in response, response = " + jsonResponse);

            List<Category> categories = context.read("$.categories", CATEGORY_DTO_TYPE_REF)
                    .stream()
                    .map(CategoryDto::toCategory)
                    .collect(Collectors.toList());
            categories.forEach(category -> category.setRestaurant(restaurantRepository.findByRestaurantId(restaurantId)
                    .orElseThrow(() -> new IllegalStateException("Restaurant must be found at this point"))));

            List<Category> result = categoryRepository.saveAll(categories);
            updateProductInformation(result);
            return result;
        } else {
            throw new IllegalStateException("Was waiting for status code 200 or 304, got response " + jsonResponse);
        }
    }

    //this method updates count left in all products ((jowiApiCountLeft) - (allActiveOrdersProductCount))
    private void updateProductInformation(List<? extends Category> categories) {
        List<Order> activeOrders = orderRepository.findAllByStateWithProducts(ACTIVE);
        List<Product> products = categories.stream().flatMap(c -> c.getProducts().stream())
                .collect(Collectors.toList());

        products.stream().filter(p -> p.getCountLeft() == -1)
                .forEach(p -> p.setCountLeft(Integer.MAX_VALUE));

        List<ProductWithCount> productWithCounts = activeOrders.stream()
                .flatMap(o -> o.getProducts().stream())
                .collect(Collectors.toList());

        for (ProductWithCount pwc : productWithCounts) {
            int index = products.indexOf(pwc.getProduct());
            if (index >= 0){
                Product product = products.get(index);
                product.setCountLeft(product.getCountLeft() - pwc.getCount());
            }
        }

        productRepository.saveAll(products);
    }

    public Optional<Category> findByNameAndRestaurantId(String name, int restaurantId) {
        return categoryRepository.findByNameAndRestaurantId(name, restaurantId);
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public List<Category> findByRestaurantStringId(String id) {
        return categoryRepository.findAllByRestaurantRestaurantId(id);
    }
}
