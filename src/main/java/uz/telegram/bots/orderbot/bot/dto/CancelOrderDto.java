package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class CancelOrderDto {
    @JsonProperty("api_key")
    private String apiKey;
    private String sig;
    @JsonProperty("cancellation_reason")
    private String cancellationReason;


}
