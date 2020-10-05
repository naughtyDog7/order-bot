package uz.telegram.bots.orderbot.bot.service.jowi;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import uz.telegram.bots.orderbot.bot.dto.ProductDto;
import uz.telegram.bots.orderbot.bot.service.OrderService;
import uz.telegram.bots.orderbot.bot.service.ProductService;
import uz.telegram.bots.orderbot.bot.service.ProductWithCountService;
import uz.telegram.bots.orderbot.bot.service.RestaurantService;
import uz.telegram.bots.orderbot.bot.user.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class JowiProductServiceImpl implements JowiProductService {
    private final ProductService productService;
    private final RestaurantService restaurantService;
    private final OrderService orderService;
    private final ProductWithCountService pwcService;

    JowiProductServiceImpl(ProductService productService, RestaurantService restaurantService,
                           OrderService orderService, ProductWithCountService pwcService) {
        this.productService = productService;
        this.restaurantService = restaurantService;
        this.orderService = orderService;
        this.pwcService = pwcService;
    }

    @Override
    public void deleteProductsRemovedFromServer(List<Product> oldProducts, List<Product> newProducts,
                                                 String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        oldProducts.stream()
                .filter(ptd -> newProducts.stream()
                        .map(Product::getProductId)
                        .noneMatch(id -> ptd.getProductId().equals(id)))
                .forEach(p -> productService.delete(p, restaurantId, bot, telegramUser));
    }

    @Override
    public void updateProductsForCategories(List<? extends Category> categories, String restaurantId) {
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
        product.setPrice((int) Math.round(productDto.getPriceForOnlineOrder()));
        product.setImageUrl(productDto.getImageUrl());
        return productService.save(product);
    }
}
