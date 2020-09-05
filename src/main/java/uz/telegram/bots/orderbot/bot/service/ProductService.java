package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<Product> getAllByCategoryId(int categoryId);
    Optional<Product> getByCategoryIdAndName(int categoryId, String name);
    Optional<Product> getByStringProductId(String id);

    Product save(Product product);
    Product fromProductWithCount(long id);

    Optional<Product> getLastChosenByOrder(Order order);
    List<Product> deleteAllByCategoryId(int categoryId);

    List<Product> saveAll(List<Product> products);

    void delete(Product product);
}
