package uz.telegram.bots.orderbot.bot.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WebhookOrderWrapperDto {
    private int status;
    private String type;
    private WebhookOrderDto data;
}
