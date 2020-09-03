package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
class CategoryMainMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService service;
    private final ProductService productService;
    private final ProductWithCountService productWithCountService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final RestaurantService restaurantService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;

    @Autowired
    CategoryMainMessageState(ResourceBundleFactory rbf, TelegramUserService service,
                             ProductService productService, ProductWithCountService productWithCountService,
                             OrderService orderService, CategoryService categoryService,
                             RestaurantService restaurantService, KeyboardFactory kf,
                             KeyboardUtil ku, LockFactory lf) {
        this.rbf = rbf;
        this.service = service;
        this.productService = productService;
        this.productWithCountService = productWithCountService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.restaurantService = restaurantService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
    }

    @Override
    //can come as product or back button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }

        String backButtonText = rb.getString("btn-back");
        String text = message.getText();
        Order order = orderService.getActive(telegramUser)
                .orElseThrow(() -> new AssertionError("Order must be present at this point"));
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Restaurant restaurant = restaurantService.getByOrderId(order.getId());
            if (text.equals(backButtonText)) {
                List<Category> categories = categoryService.findNonEmptyByRestaurantStringId(restaurant.getRestaurantId());
                int basketItemsNum = productWithCountService.getBasketItemsCount(order.getId());
                handleBack(bot, telegramUser, rb, categories, basketItemsNum);
            } else {
                Category category = categoryService.getLastChosenByOrder(order)
                        .orElseThrow(() -> new AssertionError("Category must be present at this point"));

                Optional<Product> optProduct = productService.getByCategoryIdAndName(category.getId(), text); //if present then valid product name was received in message

                optProduct.ifPresentOrElse(product -> handleProduct(bot, telegramUser, rb, product, order),
                        () -> DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb));
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleProduct(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Product product, Order order) {
        SendMessage sendMessage1 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(String.format("%s %d %s", rb.getString("this-courses-price-is"), product.getPrice(), rb.getString("uzs-text")));

        SendMessage sendMessage2 = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("choose-product-num"));

        setProductKeyboard(sendMessage2, telegramUser.getLangISO(), product);

        try {
            bot.execute(sendMessage1);
            bot.execute(sendMessage2);
            telegramUser.setCurState(TelegramUser.UserState.PRODUCT_NUM_CHOOSE);
            service.save(telegramUser);
            order.setLastChosenProduct(product);
            orderService.save(order);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setProductKeyboard(SendMessage sendMessage, String langISO, Product product) {
        ku.setProductNumChoose(sendMessage, langISO, product);
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, List<Category> categories, int basketNumItems) {
        ToOrderMainHandler.builder()
                .bot(bot)
                .kf(kf)
                .rb(rb)
                .service(service)
                .telegramUser(telegramUser)
                .ku(ku)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems, false);

    }


}
