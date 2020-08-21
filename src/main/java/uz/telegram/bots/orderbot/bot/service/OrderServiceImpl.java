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
        productService.updateProducts();
        synchronized (this) {
            List<ProductWithCount> productsWithCount = order.getProducts();
            for (ProductWithCount productWithCount : productsWithCount) {
                Product product = productWithCount.getProduct();
                product.setCountLeft(product.getCountLeft() + productWithCount.getCount());
                productWithCount.setCount(0);
                productRepository.save(product);
                productWithCountRepository.save(productWithCount);
            }
        }
    }
}
