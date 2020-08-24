package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.OrderRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductWithCountRepository;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Optional;

import static uz.telegram.bots.orderbot.bot.user.Order.OrderState.CANCELLED;

@Service
public class OrderServiceImpl implements OrderService {


    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ProductWithCountRepository productWithCountRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository repository, ProductRepository productRepository,
                            ProductService productService, ProductWithCountRepository productWithCountRepository) {
        this.orderRepository = repository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.productWithCountRepository = productWithCountRepository;
    }

    @Override
    public Optional<Order> getActive(TelegramUser user) {
        return orderRepository.findFirstByStateIsAndTelegramUser(Order.OrderState.ACTIVE, user);
    }

    @Override
    public <S extends Order> S save(S s) {
        return orderRepository.save(s);
    }

    @Override
    public void cancelOrder(Order order) {
        List<ProductWithCount> productsWithCount = productWithCountRepository.getAllByOrder(order);
        for (ProductWithCount productWithCount : productsWithCount) {
            Product product = productService.fromProductWithCount(productWithCount.getId());
            product.setCountLeft(product.getCountLeft() + productWithCount.getCount());
            productWithCount.setCount(0);
            productRepository.save(product);
            productWithCountRepository.save(productWithCount);
        }

        order.setState(CANCELLED);
        orderRepository.save(order);
    }
}
