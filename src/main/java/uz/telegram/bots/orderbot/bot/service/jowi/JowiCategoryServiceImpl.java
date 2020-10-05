package uz.telegram.bots.orderbot.bot.service.jowi;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import uz.telegram.bots.orderbot.bot.dto.CategoryDto;
import uz.telegram.bots.orderbot.bot.dto.ProductDto;
import uz.telegram.bots.orderbot.bot.service.CategoryService;
import uz.telegram.bots.orderbot.bot.service.ProductService;
import uz.telegram.bots.orderbot.bot.service.RestaurantService;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import java.io.IOException;
import java.util.*;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class JowiCategoryServiceImpl implements JowiCategoryService {
    private final UriUtil uriUtil;
    private final RestTemplate restTemplate;
    private final RestaurantService restaurantService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final JowiProductService jowiProductService;


    // this map is used to save etags
    /**
     * This map is used to save etags for restaurants
     * key -> restaurant id
     * value -> etag
     */
    private final Map<String, String> restaurantIdEtags = new HashMap<>();

    private static final TypeRef<List<CategoryDto>> CATEGORY_DTO_TYPE_REF = new TypeRef<>() {
    };

    JowiCategoryServiceImpl(UriUtil uriUtil, RestTemplate restTemplate, RestaurantService restaurantService,
                            CategoryService categoryService, ProductService productService, JowiProductService jowiProductService) {
        this.uriUtil = uriUtil;
        this.restTemplate = restTemplate;
        this.restaurantService = restaurantService;
        this.categoryService = categoryService;
        this.productService = productService;
        this.jowiProductService = jowiProductService;
    }


    /**
     * This method is used to fetch categories for specific restaurant from JOWI api
     * if anything changed, updates restaurants in repository
     *
     * @param restaurantId restaurant id which can be taken from JOWI api
     * @param bot          telegram bot used to send message in case of deleting product which was in someone's basket
     * @return loaded categories
     * @throws IllegalStateException if incorrect or invalid response received
     */
    @Override
    //this method fetches categories from jowi api,
    public List<Category> updateAndFetchNonEmptyCategories(String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser) throws IOException {
        RequestEntity<Void> requestEntity = RequestEntity.get(uriUtil.getMenuGetUri(restaurantId))
                .ifNoneMatch(restaurantIdEtags.getOrDefault(restaurantId, "noetag"))
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();

        ResponseEntity<String> jsonResponse = restTemplate.exchange(requestEntity, String.class);
        return updateCategoriesFromResponse(jsonResponse, restaurantId, bot, telegramUser);
    }

    private List<Category> updateCategoriesFromResponse(ResponseEntity<String> jsonResponse,
                                                        String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser) throws IOException {
        if (jsonResponse.getStatusCodeValue() == HttpStatus.NOT_MODIFIED.value())
            return categoryService.findNonEmptyByRestaurantStringId(restaurantId);
        if (jsonResponse.getStatusCodeValue() == HttpStatus.OK.value()) {
            restaurantIdEtags.put(restaurantId, jsonResponse.getHeaders().getETag());
            DocumentContext context = JsonPath.parse(jsonResponse.getBody());
            if (context.read("$.status", Integer.class) != 1)
                throw new IOException("Was waiting for status 1 in response, response = " + jsonResponse);
            return updateCategoriesFromJsonContext(context, restaurantId, bot, telegramUser);
        } else {
            throw new IOException("Was waiting for status code 200 or 304, got response " + jsonResponse);
        }
    }

    private List<Category> updateCategoriesFromJsonContext(DocumentContext context, String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        List<Category> newCategories = new ArrayList<>();
        List<Category> oldCategories = categoryService.findAllByRestaurantStringId(restaurantId);
        List<CategoryDto> categoryDtos;
        try {
            categoryDtos = context.read("$.categories", CATEGORY_DTO_TYPE_REF);
        } catch (PathNotFoundException e) { //path will not be found if no categories were returned from jowi server
            categoryDtos = new ArrayList<>();
        }
        for (CategoryDto dto : categoryDtos) {
            Category oldCategory = oldCategories.stream().filter(c -> c.getName().equals(dto.getTitle())).findAny().orElse(null);
            Category newCategory = getNewOrUpdateOldCategory(dto, oldCategory, restaurantId, bot, telegramUser);
            newCategories.add(newCategory);
        }

        deleteCategoriesRemovedFromServer(oldCategories, newCategories, restaurantId, bot, telegramUser);
        Restaurant restaurant = restaurantService.findByRestaurantId(restaurantId)
                .orElseThrow(() -> new AssertionError("Restaurant must be found at this point"));
        newCategories.forEach(category -> category.setRestaurant(restaurant));
        newCategories = categoryService.saveAll(newCategories);
        jowiProductService.updateProductsForCategories(newCategories, restaurantId);
        return newCategories;
    }

    private void deleteCategoriesRemovedFromServer(List<Category> oldCategories, List<Category> newCategories,
                                                   String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        oldCategories.stream()
                .filter(ctd -> newCategories.stream()
                        .map(Category::getName)
                        .noneMatch(c -> ctd.getName().equals(c)))
                .forEach(c -> {
                    productService.getAllByCategoryId(c.getId()).forEach(p -> productService.delete(p, restaurantId, bot, telegramUser));
                    categoryService.delete(c);
                });
    }

    private Category getNewOrUpdateOldCategory(CategoryDto categoryDto, Category oldCategory, String restaurantId,
                                               TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Category newOrUpdatedCategory = Objects.requireNonNullElseGet(oldCategory, Category::new);
        List<Product> oldProducts = productService.getAllByCategoryId(newOrUpdatedCategory.getId());
        List<Product> newProducts = new ArrayList<>();
        newOrUpdatedCategory.setName(categoryDto.getTitle());
        for (ProductDto productDto : categoryDto.getCourses()) {
            Product product = jowiProductService.getNewOrUpdateOldProduct(productDto, oldProducts.stream()
                    .filter(p -> p.getProductId().equals(productDto.getId()))
                    .findAny().orElse(null));
            product.setCategory(newOrUpdatedCategory);
            newProducts.add(product);
        }
        jowiProductService.deleteProductsRemovedFromServer(oldProducts, newProducts, restaurantId, bot, telegramUser);
        newOrUpdatedCategory.setProducts(newProducts);
        return newOrUpdatedCategory;
    }
}
