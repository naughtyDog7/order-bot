package uz.telegram.bots.orderbot.bot.service;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<Product> getAllByCategoryId(int categoryId);
    Optional<Product> getByCategoryIdAndName(int categoryId, String name);

    Product save(Product product);
    Product fromProductWithCount(long id);

    Optional<Product> getLastChosenByOrder(Order order);

    List<Product> saveAll(List<Product> products);

    void delete(Product product, String restaurantId, TelegramLongPollingBot bot, TelegramUser callerUser);
}
