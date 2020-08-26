package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.io.IOException;
import java.util.Optional;

public interface OrderService {
    Optional<Order> getActive(TelegramUser user);
    <S extends Order> S save(S s);
    void cancelOrder(Order order);
    void postOrder(Order order, TelegramUser user) throws IOException;
}
