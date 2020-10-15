package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.properties.AppProperties;
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
    private final ProductWithCountService pwcService;
    private final RestaurantService restaurantService;
    private final CategoryService categoryService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;
    private final AppProperties appProperties;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    ProductNumChooseState(ResourceBundleFactory rbf, TelegramUserService userService,
                          OrderService orderService, ProductService productService,
                          ProductWithCountService pwcService, RestaurantService restaurantService,
                          CategoryService categoryService, KeyboardFactory kf,
                          KeyboardUtil ku, LockFactory lf, AppProperties appProperties, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.productService = productService;
        this.pwcService = pwcService;
        this.restaurantService = restaurantService;
        this.categoryService = categoryService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
        this.appProperties = appProperties;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as number or back button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText())
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
        else
            handleMessageText(bot, telegramUser, rb, message.getText());
    }

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String messageText) {
        String btnBackText = rb.getString("btn-back");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.findActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            Product product = productService.getLastChosenByOrder(order)
                    .orElseThrow(() -> new AssertionError("Product must be present at this point"));
            Restaurant restaurant = restaurantService.findByOrderId(order.getId());
            if (messageText.equals(btnBackText)) {
                handleBack(bot, telegramUser, rb, order);
            } else {
                handlePotentialProductNum(bot, telegramUser, rb, messageText,
                        order, product, restaurant);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        Category category = categoryService.findLastChosenByOrder(order)
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

    private void handlePotentialProductNum(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                           ResourceBundle rb, String messageText,
                                           Order order, Product product, Restaurant restaurant) {
        int numOfProductInBasket = pwcService.findByOrderIdAndProductId(order.getId(), product.getId())
                .map(ProductWithCount::getCount)
                .orElse(0);
        try {
            int numChosen = Integer.parseInt(messageText);
            if (numChosen <= 0 ||
                    (numChosen + numOfProductInBasket > appProperties.getBasketCountLimit())) {
                handleIncorrectNum(bot, telegramUser, rb, restaurant, product, order);
            } else {
                handleNumChosen(bot, telegramUser, rb, restaurant, product, order, numChosen);
            }
        } catch (NumberFormatException e) {
            handleIncorrectNum(bot, telegramUser, rb, restaurant, product, order);
        }
    }

    private void handleIncorrectNum(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Restaurant restaurant, Product product, Order order) {
        if (product.getCountLeft() > 0) {
            SendMessage incorrectProductNumMessage = new SendMessage()
                    .setText(rb.getString("retry-products-num"))
                    .setChatId(telegramUser.getChatId());
            //need to update keyboard because maybe somebody already took product
            setProductKeyboard(incorrectProductNumMessage, telegramUser.getLangISO(), product);
            try {
                bot.execute(incorrectProductNumMessage);
                //no need to change state
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            handleOutOfStock(bot, telegramUser, rb, order);
        }
    }

    private void setProductKeyboard(SendMessage sendMessage, String langISO, Product product) {
        ku.setProductNumChoose(sendMessage, langISO, product);
    }

    private void handleNumChosen(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                 ResourceBundle rb, Restaurant restaurant,
                                 Product product, Order order, int numChosen) {
        int countLeft = product.getCountLeft();
        if (countLeft < 0)
            throw new AssertionError("Product should never be in negative count left");
        if (countLeft == 0) {
            handleOutOfStock(bot, telegramUser, rb, order);
        } else if (numChosen > countLeft) {
            handleNotEnough(bot, telegramUser, rb, order, product);
        } else {
            handleValidProductNumChosen(bot, telegramUser, rb, restaurant, product, order, numChosen);
        }
    }

    private void handleOutOfStock(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        SendMessage outOfStockMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("no-product-left"));
        try {
            bot.execute(outOfStockMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        handleBack(bot, telegramUser, rb, order);
    }

    private void handleNotEnough(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, Product product) {
        SendMessage notEnoughProductMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("product-num-not-enough"));

        setProductKeyboard(notEnoughProductMessage, telegramUser.getLangISO(), product);

        try {
            bot.execute(notEnoughProductMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleValidProductNumChosen(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                             ResourceBundle rb, Restaurant restaurant,
                                             Product product, Order order, int numChosen) {
        product.setCountLeft(product.getCountLeft() - numChosen);
        productService.save(product);
        ProductWithCount productWithCount = createNewOrUpdateOldPwc(product, order, numChosen);
        pwcService.save(productWithCount);
        List<Category> categories = categoryService.findNonEmptyByRestaurantStringId(restaurant.getRestaurantId());
        int basketNumItems = pwcService.getBasketItemsCount(order.getId());
        handleToOrderMain(bot, telegramUser, rb, categories, basketNumItems);
    }

    @NotNull
    private ProductWithCount createNewOrUpdateOldPwc(Product product, Order order, int numChosen) {
        ProductWithCount productWithCount;
        Optional<ProductWithCount> optProductWithCount = pwcService.findByOrderIdAndProductId(order.getId(), product.getId());
        if (optProductWithCount.isPresent()) {
            productWithCount = optProductWithCount.get();
            productWithCount.setCount(productWithCount.getCount() + numChosen);
        } else {
            productWithCount = ProductWithCount.fromOrder(order, product, numChosen);
        }
        return productWithCount;
    }

    private void handleToOrderMain(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                   ResourceBundle rb, List<Category> categories, int basketNumItems) {
        ToOrderMainHandler.builder()
                .bot(bot)
                .telegramUser(telegramUser)
                .service(userService)
                .kf(kf)
                .ku(ku)
                .rb(rb)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems, ToOrderMainHandler.CallerPlace.OTHER);
    }

}
