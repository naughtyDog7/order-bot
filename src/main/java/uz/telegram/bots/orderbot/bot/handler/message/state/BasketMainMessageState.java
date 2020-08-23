package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@Component
class BasketMainMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final ProductWithCountService productWithCountService;
    private final RestaurantService restaurantService;
    private final CategoryService categoryService;
    private final KeyboardUtil ku;
    private final KeyboardFactory kf;
    private final LockFactory lf;

    @Autowired
    BasketMainMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                           OrderService orderService, ProductService productService,
                           ProductWithCountService productWithCountService,
                           RestaurantService restaurantService, CategoryService categoryService,
                           KeyboardUtil ku, KeyboardFactory kf, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.productService = productService;
        this.productWithCountService = productWithCountService;
        this.restaurantService = restaurantService;
        this.categoryService = categoryService;
        this.ku = ku;
        this.kf = kf;
        this.lf = lf;
    }

    @Override
    //can come as some product to remove from order, or back button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        if (!message.hasText())
            return;
        String text = message.getText();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
        String btnBack = rb.getString("btn-back");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.getActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            int basketNumItems = productWithCountService.getBasketItemsCount(order.getId());
            if (text.equals(btnBack)) {
                handleBack(bot, telegramUser, rb, order, basketNumItems);
                return;
            }
            String removeProductCharText = rb.getString("remove-product-char");
            text = text.replace(removeProductCharText, "").strip(); //remove ❌ to get clean product name
            handleItemDelete(bot, telegramUser, rb, order, text, basketNumItems);
        } finally {
            lock.unlock();
        }
    }

    private void handleItemDelete(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                  ResourceBundle rb, Order order, String productName, int basketNumItems) {
        ProductWithCount productWithCount = productWithCountService.getByOrderIdAndProductName(order.getId(), productName)
                .orElseThrow(() -> new IllegalStateException("Product must be present at this point (choosing what to remove from basket)"));
        productWithCountService.deleteById(productWithCount.getId());
        //if no products left, go back to order main
        if (basketNumItems == 1) {
            handleNoItemsLeft(bot, telegramUser, rb, order);
        } else {
            handleAfterItemDeleted(bot, telegramUser, rb, order);
        }
    }

    private void handleAfterItemDeleted(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        SendMessage sendMessage = new SendMessage()
                .setText(rb.getString("deleted"))
                .setChatId(telegramUser.getChatId());
        List<ProductWithCount> products = productWithCountService.getAllFromOrderId(order.getId());
        ku.setBasketKeyboard(productService, sendMessage, products, rb, telegramUser.getLangISO());

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleNoItemsLeft(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("basket-has-been-emptied"));

        try {
            bot.execute(sendMessage);
            handleBack(bot, telegramUser, rb, order, 0);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, int basketNumItems) {
        List<Category> categories = categoryService.findByOrderId(order.getId());
        ToOrderMainHandler.builder()
                .bot(bot)
                .rb(rb)
                .service(userService)
                .ku(ku)
                .kf(kf)
                .telegramUser(telegramUser)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems);
    }
}
