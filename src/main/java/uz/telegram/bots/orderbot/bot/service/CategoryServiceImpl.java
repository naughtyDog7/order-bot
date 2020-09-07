package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.telegram.bots.orderbot.bot.repository.CategoryRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.repository.RestaurantRepository;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.util.UriUtil;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
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

    public Optional<Category> findByNameAndRestaurantId(String name, int restaurantId) {
        return categoryRepository.findByNameAndRestaurantId(name, restaurantId);
    }

    @Override
    public List<Category> findNonEmptyByRestaurantStringId(String id) {
        return categoryRepository.findNonEmptyByRestaurantStringId(id);
    }

    @Override
    public List<Category> findNonEmptyByOrderId(long orderId) {
        return categoryRepository.findNonEmptyByOrderId(orderId);
    }

    @Override
    public Optional<Category> findLastChosenByOrder(Order order) {
        return categoryRepository.findLastChosenByOrderId(order.getId());
    }

    @Override
    public List<Category> findAllByRestaurantStringId(String id) {
        return categoryRepository.findAllByRestaurantRestaurantId(id);
    }

    @Override
    public List<Category> saveAll(List<Category> categories) {
        return categoryRepository.saveAll(categories);
    }

    @Override
    public void delete(Category category) {
        categoryRepository.delete(category);
    }
}
