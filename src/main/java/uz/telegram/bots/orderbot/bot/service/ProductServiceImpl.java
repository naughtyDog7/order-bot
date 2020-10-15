package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.repository.OrderRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.repository.ProductWithCountRepository;
import uz.telegram.bots.orderbot.bot.repository.RestaurantRepository;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

import static uz.telegram.bots.orderbot.bot.user.Order.OrderState.ACTIVE;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final RestaurantRepository restaurantRepository;
    private final ProductWithCountRepository pwcRepository;
    private final OrderRepository orderRepository;
    private final TelegramUserService userService;
    private final ResourceBundleFactory rbf;
    private final LockFactory lf;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, RestaurantRepository restaurantRepository,
                              ProductWithCountRepository pwcRepository, OrderRepository orderRepository,
                              TelegramUserService userService, ResourceBundleFactory rbf, LockFactory lf) {
        this.productRepository = productRepository;
        this.restaurantRepository = restaurantRepository;
        this.pwcRepository = pwcRepository;
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.rbf = rbf;
        this.lf = lf;
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
        return productRepository.findByProductWithCountId(id);
    }

    @Override
    public Optional<Product> getLastChosenByOrder(Order order) {
        return productRepository.getByProductId(order.getLastChosenProductStringId());
    }

    @Override
    public List<Product> saveAll(List<Product> products) {
        return productRepository.saveAll(products);
    }

    @Override
    public void delete(Product product, String restaurantId, TelegramLongPollingBot bot, TelegramUser callerUser) {
        deletePwcsWithCurrentProduct(product, restaurantId, bot);
        productRepository.delete(product);
    }

    private void deletePwcsWithCurrentProduct(Product product, String restaurantId, TelegramLongPollingBot bot) {
        List<ProductWithCount> pwcsToDelete =
                pwcRepository.findAllToDeleteByRestaurantAndProduct(restaurantId, product.getProductId(), ACTIVE);
        for (ProductWithCount pwc : pwcsToDelete) {
            Order order = orderRepository.findByProductWithCountId(pwc.getId());
            TelegramUser telegramUser = userService.findByOrderId(order.getId());
            Lock lock = lf.getLockForChatId(telegramUser.getChatId());
            try {
                lock.lock();
                pwcRepository.delete(pwc);
                notifyUsersAboutDelete(product, telegramUser, bot);
            } finally {
                lock.unlock();
            }
        }
    }

    private void notifyUsersAboutDelete(Product product, TelegramUser telegramUser, TelegramLongPollingBot bot) {
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        SendMessage productRemovedMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("product-on-server-deleted-and-removed-from-basket")
                        .replace("{productName}", product.getName()));
        try {
            bot.execute(productRemovedMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
    