package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.KeyboardUtil;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@Component
class LocationSendState implements MessageState {
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final PaymentInfoService paymentInfoService;
    private final LocationService locationService;
    private final CategoryService categoryService;
    private final ProductWithCountService pwcService;
    private final KeyboardFactory kf;
    private final KeyboardUtil ku;
    private final LockFactory lf;

    @Autowired
    LocationSendState(ResourceBundleFactory rbf, TelegramUserService userService,
                      OrderService orderService, PaymentInfoService paymentInfoService,
                      LocationService locationService, CategoryService categoryService, ProductWithCountService pwcService,
                      KeyboardFactory kf, KeyboardUtil ku, LockFactory lf) {
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.paymentInfoService = paymentInfoService;
        this.locationService = locationService;
        this.categoryService = categoryService;
        this.pwcService = pwcService;
        this.kf = kf;
        this.ku = ku;
        this.lf = lf;
    }

    @Override
    //can come as location, back button
    public void handle(Update update, TelegramLongPollingBot bot, TelegramUser telegramUser) {
        Message message = update.getMessage();
        ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());

        Lock lock = lf.getResourceLock();
        try {
            lock.lock();
            Order order = orderService.getActive(telegramUser)
                    .orElseThrow(() -> new AssertionError("Order must be present at this point"));
            if (message.hasText()) {
                String text = message.getText();
                String btnBack = rb.getString("btn-back");
                if (text.equals(btnBack))
                    handleBack(bot, telegramUser, rb, order);
                else
                    DefaultBadRequestHandler.handleContactBadRequest(bot, telegramUser, rb);
            } else if (message.hasLocation()) {
                Location location = message.getLocation();
                handleLocation(bot, telegramUser, rb, order, location);
            } else {
                DefaultBadRequestHandler.handleLocationBadRequest(bot, telegramUser, rb);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleLocation(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order, Location location) {
        TelegramLocation tLocation = TelegramLocation.of(location.getLongitude(), location.getLatitude());
        System.out.println(location);
        System.out.println(tLocation);
        PaymentInfo paymentInfo = paymentInfoService.getFromOrderId(order.getId());
        paymentInfo.setOrderLocation(tLocation);
        PaymentInfo pi = paymentInfoService.save(paymentInfo);
        System.out.println(locationService.findByPaymentInfoId(pi.getId()));
    }

    private void handleBack(TelegramLongPollingBot bot, TelegramUser telegramUser, ResourceBundle rb, Order order) {
        List<Category> categories = categoryService.findNonEmptyByOrderId(order.getId());
        int basketNumItems = pwcService.getBasketItemsCount(order.getId());
        ToOrderMainHandler.builder()
                .bot(bot)
                .service(userService)
                .rb(rb)
                .telegramUser(telegramUser)
                .ku(ku)
                .kf(kf)
                .categories(categories)
                .build()
                .handleToOrderMain(basketNumItems);
    }
}
