package uz.telegram.bots.orderbot.bot.handler.message.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
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
    private final BadRequestHandler badRequestHandler;

    @Autowired
    CategoryMainMessageState(ResourceBundleFactory rbf, TelegramUserService service,
                             ProductService productService, ProductWithCountService productWithCountService,
                             OrderService orderService, CategoryService categoryService,
                             RestaurantService restaurantService, KeyboardFactory kf,
                             KeyboardUtil ku, LockFactory lf, BadRequestHandler badRequestHandler) {
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
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as product or back button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        if (!message.hasText()) {
            badRequestHandler.handleTextBadRequest(bot, telegramUser, rb);
            return;
        }
        String messageText = message.getText();
        handleMessageText(bot, telegramUser, rb, messageText);
    }

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                   ResourceBundle rb, String messageText) {
        String backButtonText = rb.getString("btn-back");
        Order order = orderService.findActive(telegramUser)
                .orElseThrow(() -> new AssertionError("Order must be present at this point"));
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Restaurant restaurant = restaurantService.findByOrderId(order.getId());
            if (messageText.equals(backButtonText)) {
                getCategoriesAndHandleBack(bot, telegramUser, rb, order, restaurant);
            } else {
                handlePotentialProductName(bot, telegramUser, rb, messageText, order);
            }
        } finally {
            lock.unlock();
        }
    }

    private void getCategoriesAndHandleBack(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                            ResourceBundle rb, Order order, Restaurant restaurant) {
        List<Category> categories
                = categoryService.findNonEmptyByRestaurantStringId(restaurant.getRestaurantId());
        int basketItemsNum = productWithCountService.getBasketItemsCount(order.getId());
        handleBack(bot, telegramUser, rb, categories, basketItemsNum);
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser,
                            ResourceBundle rb, List<Category> categories, int basketNumItems) {
        ToOrderMainHandler.builder()
                .bot(bot)
                .kf(kf)
                .rb(rb)
                .service(service)
                .telegramUser(telegramUser)
                .ku(ku)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems, ToOrderMainHandler.CallerPlace.OTHER);

    }

    private void handlePotentialProductName(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                            ResourceBundle rb, String messageText, Order order) {
        Category currentCategory = categoryService.findLastChosenByOrder(order)
                .orElseThrow(() -> new AssertionError("Category must be present at this point"));
        Optional<Product> optProduct
                = productService.getByCategoryIdAndName(currentCategory.getId(), messageText);
        //if present then valid product name was received in message
        optProduct.ifPresentOrElse(product -> handleProduct(bot, telegramUser, rb, product, order),
                () -> badRequestHandler.handleTextBadRequest(bot, telegramUser, rb));
    }

    private void handleProduct(TelegramLongPollingBot bot, TelegramUser telegramUser,
                               ResourceBundle rb, Product product, Order order) {
        try {
            sendProductTitleMessage(bot, telegramUser, rb, product);
            sendChooseProductNumberMessage(bot, telegramUser, rb, product);
            telegramUser.setCurState(UserState.PRODUCT_NUM_CHOOSE);
            service.save(telegramUser);
            order.setLastChosenProductStringId(product.getProductId());
            orderService.save(order);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendProductTitleMessage(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                         ResourceBundle rb, Product product) throws TelegramApiException {
        String imageUrl = product.getImageUrl();
        try {
            trySendPhoto(bot, telegramUser, rb, product, imageUrl);
        } catch (IOException e) {
            handleProductPhotoUnavailable(bot, telegramUser, rb, product);
        }
    }

    private void trySendPhoto(TelegramLongPollingBot bot, TelegramUser telegramUser,
                              ResourceBundle rb, Product product, String imageUrl) throws IOException, TelegramApiException {
        URL url = new URL(imageUrl);
        URLConnection urlConnection = url.openConnection();
        SendPhoto sendPhoto = new SendPhoto()
                .setPhoto(product.getName(), urlConnection.getInputStream())
                .setChatId(telegramUser.getChatId())
                .setCaption(String.format("%s: %d %s", rb.getString("price"), product.getPrice(), rb.getString("uzs-text")));
        bot.execute(sendPhoto);
    }

    private void handleProductPhotoUnavailable(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Product product) {
        SendMessage productTitleMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(String.format("%s %d %s", rb.getString("this-courses-price-is"), product.getPrice(), rb.getString("uzs-text")));
        try {
            bot.execute(productTitleMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendChooseProductNumberMessage(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                                ResourceBundle rb, Product product) throws TelegramApiException {
        SendMessage chooseNumberMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("choose-product-num"));
        setProductKeyboard(chooseNumberMessage, telegramUser.getLangISO(), product);
        bot.execute(chooseNumberMessage);
    }

    private void setProductKeyboard(SendMessage sendMessage, String langISO, Product product) {
        ku.setProductNumChoose(sendMessage, langISO, product);
    }


}
