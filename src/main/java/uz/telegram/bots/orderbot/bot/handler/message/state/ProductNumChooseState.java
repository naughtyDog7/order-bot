package uz.telegram.bots.orderbot.bot.handler.message.state;

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
class ProductNumChooseState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final ProductWithCountService productWithCountService;
    private final RestaurantService restaurantService;
    private final CategoryService categoryService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;

    @Autowired
    ProductNumChooseState(ResourceBundleFactory rbf, TelegramUserService userService,
                          OrderService orderService, ProductService productService,
                          ProductWithCountService productWithCountService, RestaurantService restaurantService,
                          CategoryService categoryService, KeyboardFactory kf, KeyboardUtil ku, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.productService = productService;
        this.productWithCountService = productWithCountService;
        this.restaurantService = restaurantService;
        this.categoryService = categoryService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
    }

    @Override
    //can come as number or back button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            DefaultBadRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }
        String text = message.getText();
        String btnBackText = rb.getString("btn-back");

        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.getActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            Product product = productService.getByStringProductId(order.getChosenProductStringId())
                    .orElseThrow(() -> new AssertionError("Product must be present at this point"));
            Restaurant restaurant = restaurantService.getByOrderId(order.getId());
            if (text.equals(btnBackText)) {
                handleBack(bot, telegramUser, rb, order, restaurant);
                return;
            }

            int numChosen;
            try {
                numChosen = Integer.parseInt(text);
                if (numChosen <= 0) {
                    handleIncorrectNum(bot, telegramUser, rb, restaurant, product, order);
                    return;
                }
            } catch (NumberFormatException e) {
                handleIncorrectNum(bot, telegramUser, rb, restaurant, product, order);
                return;
            }


            handleNumChosen(bot, telegramUser, rb, restaurant, product, order, numChosen);
        } finally {
            lock.unlock();
        }
    }

    private void handleNumChosen(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb,
                                 Restaurant restaurant, Product product, Order order, int numChosen) {
        int countLeft = product.getCountLeft();
        if (countLeft < 0)
            throw new AssertionError("Product should never be in negative count left");
        if (countLeft == 0) {
            handleOutOfStock(bot, telegramUser, rb, order, restaurant);
            return;
        }
        if (numChosen > countLeft) {
            handleNotEnough(bot, telegramUser, rb, order, product);
            return;
        }
        product.setCountLeft(product.getCountLeft() - numChosen);
        productService.save(product);

        ProductWithCount productWithCount;
        Optional<ProductWithCount> optProductWithCount = productWithCountService.findByOrderIdAndProductId(order.getId(), product.getId());
        if (optProductWithCount.isPresent()) {
            productWithCount = optProductWithCount.get();
            productWithCount.setCount(productWithCount.getCount() + numChosen);
        } else {
            productWithCount = ProductWithCount.fromOrder(order, product, numChosen);
        }

        productWithCountService.save(productWithCount);
        List<Category> categories = categoryService.findNonEmptyByRestaurantStringId(restaurant.getRestaurantId());
        int basketNumItems = productWithCountService.getBasketItemsCount(order.getId());
        ToOrderMainHandler.builder()
                .bot(bot)
                .telegramUser(telegramUser)
                .service(userService)
                .kf(kf)
                .ku(ku)
                .rb(rb)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems);
    }

    private void handleNotEnough(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, Product product) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("product-num-not-enough"));

        setProductKeyboard(sendMessage, telegramUser.getLangISO(), product);

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleOutOfStock(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, Restaurant restaurant) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("no-product-left"));
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        handleBack(bot, telegramUser, rb, order, restaurant);
    }

    private void handleIncorrectNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Restaurant restaurant, Product product, Order order) {
        if (product.getCountLeft() > 0) {
            SendMessage sendMessage = new SendMessage()
                    .setText(rb.getString("retry-products-num"))
                    .setChatId(telegramUser.getChatId());
            //need to update keyboard because maybe somebody already took product
            setProductKeyboard(sendMessage, telegramUser.getLangISO(), product);
            try {
                bot.execute(sendMessage);
                //no need to change state
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            handleOutOfStock(bot, telegramUser, rb, order, restaurant);
        }
    }

    private void setProductKeyboard(SendMessage sendMessage, String langISO, Product product) {
        ku.setProductNumChoose(sendMessage, langISO, product);
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, Restaurant restaurant) {
        Category category = categoryService.findByNameAndRestaurantId(order.getChosenCategoryName(), restaurant.getId())
                .orElseThrow(() -> new AssertionError("Category must be present at this point"));
        List<Product> products = productService.getAllByCategoryId(category.getId());
        ToCategoryMainHandler.builder()
                .bot(bot)
                .userService(userService)
                .telegramUser(telegramUser)
                .rb(rb)
                .kf(kf)
                .ku(ku)
                .products(products)
                .build()
                .handleToProducts();
    }

}
