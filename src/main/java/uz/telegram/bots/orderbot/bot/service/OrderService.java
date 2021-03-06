package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Restaurant;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Optional<Order> findActive(TelegramUser user);

    <S extends Order> S save(S s);

    Optional<Order> findByOrderStringId(String orderId);

    void deleteOrder(Order order);

    List<Order> findActiveForRestaurant(Restaurant restaurant);

    int getProductsPrice(long id);
}
