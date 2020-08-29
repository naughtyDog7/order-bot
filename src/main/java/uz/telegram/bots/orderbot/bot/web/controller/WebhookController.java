package uz.telegram.bots.orderbot.bot.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderDto;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderDto.WebhookOrderStatus;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderWrapperDto;
import uz.telegram.bots.orderbot.bot.service.OrderService;
import uz.telegram.bots.orderbot.bot.service.TelegramUserService;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;
import uz.telegram.bots.orderbot.bot.util.LockFactory;
import uz.telegram.bots.orderbot.bot.util.ResourceBundleFactory;

import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;

@RestController("/webhook")
@Slf4j
public class WebhookController {

    private final TelegramLongPollingBot bot;
    private final ResourceBundleFactory rbf;
    private final TelegramUserService userService;
    private final OrderService orderService;
    private final LockFactory lf;

    @Autowired
    public WebhookController(TelegramLongPollingBot bot, ResourceBundleFactory rbf,
                             TelegramUserService userService, OrderService orderService, LockFactory lf) {
        this.bot = bot;
        this.rbf = rbf;
        this.userService = userService;
        this.orderService = orderService;
        this.lf = lf;
    }

    @PostMapping(value = "/", produces = "text/plain")
    public String proceedWebhook(@RequestBody WebhookOrderWrapperDto orderWrapper) {
        if (orderWrapper.getStatus() != 1) {
            log.error("Received webhook with other than 1 status, orderWrapperDto " + orderWrapper);
            return null;
        }
        WebhookOrderDto orderDto = orderWrapper.getData();
        if (orderDto == null) {
            log.error("Received null orderDto, orderWrapperDto = " + orderWrapper);
        }
        proceedOrderUpdate(orderDto);
        return "OK";
    }

    private void proceedOrderUpdate(WebhookOrderDto orderDto) {
        Order order = orderService.getByOrderStringId(orderDto.getOrderId())
                .orElseThrow(() -> new AssertionError("Order must be present when receiving webhook"));
        TelegramUser telegramUser = userService.getByOrderId(order.getId());
        Lock lock = lf.getLockForChatId(telegramUser.getChatId());
        try {
            lock.lock();
            ResourceBundle rb = rbf.getMessagesBundle(telegramUser.getLangISO());
            WebhookOrderStatus status = orderDto.getStatus();
            switch (status) {
                case ACCEPTED:
                    handleAccepted(telegramUser, order, rb);
                    break;
                case CANCELLED:
                    handleCancelled(telegramUser, order, rb);
                    break;
                case SENT:
                    handleSent(telegramUser, order, rb);
                    break;
                case DELIVERED:
                    handleDelivered(telegramUser, order, rb);
                    break;
            }
        } finally {
            lock.unlock();
        }

    }

    private void handleAccepted(TelegramUser telegramUser, Order order, ResourceBundle rb) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(telegramUser.getChatId())
                .setText(rb.getString(""));
    }

    private void handleDelivered(TelegramUser telegramUser, Order order, ResourceBundle rb) {

    }

    private void handleSent(TelegramUser telegramUser, Order order, ResourceBundle rb) {

    }

    private void handleCancelled(TelegramUser telegramUser, Order order, ResourceBundle rb) {

    }
}


