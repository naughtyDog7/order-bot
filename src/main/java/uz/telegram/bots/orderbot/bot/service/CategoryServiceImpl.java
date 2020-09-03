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
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.repository.RestaurantRepository;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.io.IOException;
import java.util.*;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;
    private final UriUtil uriUtil;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository, RestaurantRepository restaurantRepository,
                               ProductService productService, ProductRepository productRepository,
                               RestTemplate restTemplate, UriUtil uriUtil) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.productService = productService;
        this.productRepository = productRepository;
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
    public List<Category> updateAndFetchNonEmptyCategories(String restaurantId) throws IOException {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getMenuGetUri(restaurantId))
                .ifNoneMatch(restaurantIdEtags.getOrDefault(restaurantId, "noetag"))
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();

        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);
        if (jsonResponse.getStatusCodeValue() == HttpStatus.NOT_MODIFIED.value()) {
            return categoryRepository.findNonEmptyByRestaurantStringId(restaurantId);


        } else if (jsonResponse.getStatusCodeValue() == HttpStatus.OK.value()) {
            restaurantIdEtags.put(restaurantId, jsonResponse.getHeaders().getETag());
            DocumentContext context = JsonPath.parse(jsonResponse.getBody());
            if (context.read("$.status", Integer.class) != 1)
                throw new IOException("Was waiting for status 1 in response, response = " + jsonResponse);

            List<Category> categories = new ArrayList<>();
            for (CategoryDto dto : context.read("$.categories", CATEGORY_DTO_TYPE_REF)) {
                Category oldCategory = categoryRepository.findByNameAndRestaurantRestaurantId(dto.getTitle(), restaurantId).orElse(null);
                Category category = CategoryDto.getNewOrUpdateOld(dto, oldCategory, productRepository);
                categories.add(category);
            }
            categories.forEach(category -> category.setRestaurant(restaurantRepository.findByRestaurantId(restaurantId)
                    .orElseThrow(() -> new AssertionError("Restaurant must be found at this point"))));

            List<Category> result = categoryRepository.saveAll(categories);
            productService.updateProductInformation(result);
            return result;
        } else {
            throw new IOException("Was waiting for status code 200 or 304, got response " + jsonResponse);
        }
    }

    public Optional<Category> findByNameAndRestaurantId(String name, int restaurantId) {
        return categoryRepository.findByNameAndRestaurantId(name, restaurantId);
    }

    @Override
    public List<Category> findNonEmptyByRestaurantStringId(String id) {
        return categoryRepository.findNonEmptyByRestaurantStringId(id);
    }

    @Override
    public List<Category> findNonEmptyByOrderId(long orderId) {
        return categoryRepository.findAllNonEmptyByOrderId(orderId);
    }
}
