package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.*;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@Component
class BasketMainMessageState implements MessageState {

    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final ProductWithCountService productWithCountService;
    private final CategoryService categoryService;
    private final KeyboardUtil ku;
    private final KeyboardFactory kf;
    private final LockFactory lf;
    private final TextUtil tu;
    private final BadRequestHandler badRequestHandler;

    @Autowired
    BasketMainMessageState(ResourceBundleFactory rbf, TelegramUserService userService,
                           OrderService orderService, ProductService productService,
                           ProductWithCountService productWithCountService,
                           CategoryService categoryService,
                           KeyboardUtil ku, KeyboardFactory kf, LockFactory lf,
                           TextUtil tu, BadRequestHandler badRequestHandler) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.productService = productService;
        this.productWithCountService = productWithCountService;
        this.categoryService = categoryService;
        this.ku = ku;
        this.kf = kf;
        this.lf = lf;
        this.tu = tu;
        this.badRequestHandler = badRequestHandler;
    }

    @Override
    //can come as some product to remove from order, or back button
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

    private void handleMessageText(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, String messageText) {
        String btnBack = rb.getString("btn-back");
        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.findActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            int basketNumItems = productWithCountService.getBasketItemsCount(order.getId());
            if (messageText.equals(btnBack))
                handleBack(bot, telegramUser, rb, order, basketNumItems);
            else
                handlePotentialProductDelete(bot, telegramUser, rb, order, basketNumItems, messageText);
        } finally {
            lock.unlock();
        }
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, int basketNumItems) {
        List<Category> categories = categoryService.findNonEmptyByOrderId(order.getId());
        ToOrderMainHandler.builder()
                .bot(bot)
                .rb(rb)
                .service(userService)
                .ku(ku)
                .kf(kf)
                .telegramUser(telegramUser)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems, ToOrderMainHandler.CallerPlace.OTHER);
    }

    private void handlePotentialProductDelete(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                              ResourceBundle rb, Order order, int basketNumItems, String messageText) {
        String removeProductCharText = rb.getString("remove-product-char");
        String potentialProductName
                = messageText.replace(removeProductCharText, "").strip(); //remove ❌ to get clean product name

        Optional<ProductWithCount> optProduct
                = productWithCountService.findByOrderIdAndProductName(order.getId(), potentialProductName);
        optProduct.ifPresentOrElse(product -> handleItemDelete(bot, telegramUser, rb, order, product, basketNumItems),
                () -> badRequestHandler.handleTextBadRequest(bot, telegramUser, rb));
    }

    private void handleItemDelete(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                  ResourceBundle rb, Order order, ProductWithCount productWithCount, int basketNumItems) {
        Product product = deletePwcFromBasket(productWithCount);
        //if no products left, go back to order main
        if (basketNumItems - 1 == 0) {
            handleNoItemsLeft(bot, telegramUser, rb, order);
        } else {
            handleAfterItemDeleted(bot, telegramUser, rb, order, product.getName());
        }
    }

    @NotNull
    private Product deletePwcFromBasket(ProductWithCount productWithCount) {
        int count = productWithCount.getCount();
        Product product = productService.fromProductWithCount(productWithCount.getId());
        product.setCountLeft(product.getCountLeft() + count);
        productService.save(product);
        productWithCountService.deleteById(productWithCount.getId());
        return product;
    }

    private void handleNoItemsLeft(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        SendMessage noItemsLeftInBasketMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("basket-has-been-emptied"));
        try {
            bot.execute(noItemsLeftInBasketMessage);
            handleBack(bot, telegramUser, rb, order, 0);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleAfterItemDeleted(TelegramLongPollingBot bot, TelegramUser telegramUser,
                                        ResourceBundle rb, Order order, String productName) {
        SendMessage productDeletedMessage = new SendMessage()
                .setText(rb.getString("deleted") + "❌ " + productName)
                .setChatId(telegramUser.getChatId());
        List<ProductWithCount> products = productWithCountService.findByOrderId(order.getId());
        try {
            bot.execute(productDeletedMessage);
            sendUpdatedBasketMessage(bot, telegramUser, rb, products);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendUpdatedBasketMessage(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, List<ProductWithCount> products) throws TelegramApiException {
        String text = tu.appendProducts(new StringBuilder(), products, rb, false, -1).toString();
        SendMessage updatedBasketMessage = new SendMessage()
                .setText(text)
                .setChatId(telegramUser.getChatId());
        ku.setBasketKeyboard(productService, updatedBasketMessage,
                products, rb, telegramUser.getLangISO());
        bot.execute(updatedBasketMessage);
    }
}
