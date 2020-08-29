package uz.telegram.bots.orderbot.bot.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderDto;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderWrapperDto;
import uz.telegram.bots.orderbot.bot.service.OrderService;

@RestController("/webhook")
@Slf4j
public class WebhookController {

    private final OrderService orderService;

    @Autowired
    public WebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping(value = "/", produces = "text/plain")
    public String proceedWebhook(@RequestBody WebhookOrderWrapperDto orderWrapper) {
        if (orderWrapper.getStatus() != 1) {
            log.error("Received webhook with other than 1 status, orderWrapper " + orderWrapper);
            return null;
        }
        WebhookOrderDto orderDto = orderWrapper.getData();
        orderService.getByOrderStringId(orderDto.getOrderId())
                .ifPresentOrElse(order -> orderService.proceedOrderUpdate(order, orderDto.getStatus()),
                        () -> log.error("Couldn't find order by webhook id, orderWrapper: " + orderWrapper));
        return "OK";
    }
}
