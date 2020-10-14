package uz.telegram.bots.orderbot.bot.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderDto;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderDto.WebhookOrderStatus;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderWrapperDto;
import uz.telegram.bots.orderbot.bot.handler.message.state.ToMainMenuHandler;
import uz.telegram.bots.orderbot.bot.properties.PaymentProperties;
import uz.telegram.bots.orderbot.bot.service.*;
import uz.telegram.bots.orderbot.bot.user.*;
import uz.telegram.bots.orderbot.bot.user.PaymentInfo.PaymentMethod;
import uz.telegram.bots.orderbot.bot.util.KeyboardFactory;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

import static uz.telegram.bots.orderbot.bot.user.PaymentInfo.PaymentMethod.CLICK;
import static uz.telegram.bots.orderbot.bot.user.PaymentInfo.PaymentMethod.PAYME;
import static uz.telegram.bots.orderbot.bot.handler.message.state.UserState.ONLINE_PAYMENT;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {

    private final TelegramLongPollingBot bot;
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final PaymentInfoService paymentInfoService;
    private final ProductWithCountService pwcService;
    private final ProductService productService;
    private final KeyboardFactory kf;
    private final LockFactory lf;
    private final PaymentProperties paymentProperties;

    @Autowired
    public WebhookController(TelegramLongPollingBot bot, ResourceBundleFactory rbf,
                             TelegramUserService userService, OrderService orderService,
                             PaymentInfoService paymentInfoService, ProductWithCountService pwcService,
                             ProductService productService, KeyboardFactory kf,
                             LockFactory lf, PaymentProperties paymentProperties) {
        this.bot = bot;
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.paymentInfoService = paymentInfoService;
        this.pwcService = pwcService;
        this.productService = productService;
        this.kf = kf;
        this.lf = lf;
        this.paymentProperties = paymentProperties;
    }

    @PostMapping(value = "/", produces = "text/plain")
    public String proceedWebhook(@RequestBody WebhookOrderWrapperDto orderWrapper) {
        if (orderWrapper.getStatus() != 1) {
            log.error("Received webhook with other than 1 status, orderWrapperDto " + orderWrapper);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status was not 1");
        }
        WebhookOrderDto orderDto = orderWrapper.getData();
        if (orderDto == null) {
            log.error("Received null orderDto, orderWrapperDto = " + orderWrapper);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order was empty");
        }
        proceedOrderUpdate(orderDto);
        return "OK";
    }

    //      0 	Новый заказ
    //      1 	Заказ принят в работу
    //      2 	Заказ отменен
    //      3 	Заказ отправлен клиенту
    //      4 	Заказ доставлен

    private void proceedOrderUpdate(WebhookOrderDto orderDto) {
        Order order = orderService.findByOrderStringId(orderDto.getOrderId())
                .orElseThrow(() -> new AssertionError("Order must be present when receiving webhook"));
        TelegramUser telegramUser = userService.findByOrderId(order.getId());
        Lock lock = lf.getLockForChatId(telegramUser.getChatId());
        try {
            lock.lock();
            ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
            WebhookOrderStatus status = orderDto.getStatus();
            log.info("Order with id " + orderDto.getOrderId() + " received webhook with status " + orderDto.getStatus());
            switch (status) {
                case ACCEPTED:
                    handleAccepted(telegramUser, order, rb);
                    break;
                case CANCELLED:
                    handleCancelled(telegramUser, order, rb);
                    break;
                case SENT:
                    handleSent(telegramUser, rb);
                    break;
                case DELIVERED:
                    handleDelivered(order);
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleAccepted(TelegramUser telegramUser, Order order, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("order-is-accepted-from-server"));
        try {
            bot.execute(sendMessage);
            PaymentInfo pi = paymentInfoService.findByOrderId(order.getId());
            PaymentMethod paymentMethod = pi.getPaymentMethod();
            if (paymentMethod == CLICK || paymentMethod == PAYME) {
                handleOnline(telegramUser, order, rb, paymentMethod);
            } else {
                handleCash(telegramUser, rb);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCash(TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("after-success-order"));
        try {
            bot.execute(sendMessage);
            ToMainMenuHandler.builder()
                    .telegramUser(telegramUser)
                    .service(userService)
                    .bot(bot)
                    .rb(rb)
                    .kf(kf)
                    .build()
                    .handleToMainMenu();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleOnline(TelegramUser telegramUser, Order order, ResourceBundle rb, PaymentMethod paymentMethod) {
        String paymentMethodString = rb.getString(paymentMethod.getRbValue());
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("now-we-will-send-you-bill").replace("{payment-method}", paymentMethodString))
                .setReplyMarkup(new ReplyKeyboardRemove());
        SendInvoice sendInvoice = createInvoice(telegramUser, order, rb, paymentMethod);
        try {
            bot.execute(sendMessage);
            bot.execute(sendInvoice);
            telegramUser.setCurState(ONLINE_PAYMENT);
            userService.save(telegramUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private SendInvoice createInvoice(TelegramUser telegramUser, Order order, ResourceBundle rb, PaymentMethod paymentMethod) {
        List<LabeledPrice> prices = new ArrayList<>();
        for (ProductWithCount pwc : pwcService.findByOrderId(order.getId())) {
            Product product = productService.fromProductWithCount(pwc.getId());
            prices.add(new LabeledPrice(product.getName() + "*" + pwc.getCount(), product.getPrice() * 100 * pwc.getCount()));
        }
        prices.add(new LabeledPrice(rb.getString("delivery"), order.getDeliveryPrice()));
        return new SendInvoice()
                .setChatId(Math.toIntExact(telegramUser.getChatId()))
                .setTitle(rb.getString("bill-title"))
                .setPayload("payload")
                .setDescription(rb.getString("bill-description"))
                .setStartParameter("start-parameter")
                .setCurrency("UZS")
                .setPrices(prices)
                .setProviderToken(paymentProperties.getByPaymentMethod(paymentMethod)
                        .orElseThrow(() -> new AssertionError("payment token not provided for method = " + paymentMethod)));
    }

    private void handleDelivered(Order order) {
        orderService.deleteOrder(order);
    }

    private void handleSent(TelegramUser telegramUser, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setText(rb.getString("your-order-is-sent"))
                .setChatId(telegramUser.getChatId());
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCancelled(TelegramUser telegramUser, Order order, ResourceBundle rb) {
        log.info("Order cancelled");
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString("order-was-cancelled"));
        try {
            bot.execute(sendMessage);
            orderService.deleteOrder(order);
            ToMainMenuHandler.builder()
                    .telegramUser(telegramUser)
                    .service(userService)
                    .bot(bot)
                    .kf(kf)
                    .rb(rb)
                    .build()
                    .handleToMainMenu();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}


