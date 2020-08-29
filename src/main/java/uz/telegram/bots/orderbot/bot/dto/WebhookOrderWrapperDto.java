package uz.telegram.bots.orderbot.bot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookOrderWrapperDto {
    private int status;
    private String type;
    private WebhookOrderDto data;
}
