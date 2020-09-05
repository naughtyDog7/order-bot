package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.OrderRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Product;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
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

    @Override
    public Optional<Product> getLastChosenByOrder(Order order) {
        return productRepository.getByProductId(order.getLastChosenProductStringId());
    }

    public List<Product> deleteAllByCategoryId(int categoryId) {
        return productRepository.deleteAllByCategoryId(categoryId);
    }

    @Override
    public List<Product> saveAll(List<Product> products) {
        return productRepository.saveAll(products);
    }

    @Override
    public void delete(Product product) {
        productRepository.delete(product);
    }
}

