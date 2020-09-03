package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.OrderRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uz.telegram.bots.orderbot.bot.user.Order.OrderState.ACTIVE;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public List<Product> getAllByCategoryId(int categoryId) {
        return productRepository.getAllByCategoryId(categoryId);
    }

    public Optional<Product> getByCategoryIdAndName(int categoryId, String name) {
        return productRepository.getByCategoryIdAndName(categoryId, name);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product fromProductWithCount(long id) {
        return productRepository.getFromProductWithCountId(id);
    }

    @Override
    public Optional<Product> getByStringProductId(String id) {
        return productRepository.getByProductId(id);
    }

    //this method updates count left in all products ((jowiApiCountLeft) - (allActiveOrdersProductCount))
    @Override
    public void updateProductInformation(List<? extends Category> categories) {
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

    @Override
    public Optional<Product> getLastChosenByOrder(Order order) {
        return productRepository.getLastChosenByOrderId(order.getId());
    }
}

