package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.OrderRepository;
import uz.telegram.bots.orderbot.bot.user.OrderU;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository repository;

    @Autowired
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Optional<OrderU> getActive(TelegramUser user) {
        return repository.findFirstByStateIsAndTelegramUser(OrderU.OrderState.ACTIVE, user);
    }

    public <S extends OrderU> S save(S s) {
        return repository.save(s);
    }
}
