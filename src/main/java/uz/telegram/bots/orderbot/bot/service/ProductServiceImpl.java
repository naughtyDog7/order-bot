package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Product;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repository;

    @Autowired
    public ProductServiceImpl(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Product> updateAndFetchProductsForCategory(Category category) {
        return null;
    }

    @Override
    public List<Product> getAllByCategoryId(int categoryId) {
        return repository.getAllByCategoryId(categoryId);
    }

    public Optional<Product> getByCategoryIdAndName(int categoryId, String name) {
        return repository.getByCategoryIdAndName(categoryId, name);
    }

    @Override
    public Product save(Product product) {
        return repository.save(product);
    }

    @Override
    public Product fromProductWithCount(long id) {
        return repository.getFromProductWithCountId(id);
    }

    @Override
    public Optional<Product> getByStringProductId(String id) {
        return repository.getByProductId(id);
    }
}

