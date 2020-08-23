package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.user.Product;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ProductDto {
    private String id;
    private String title;
    private double price;

    @JsonProperty("price_for_online_order")
    private double priceForOnlineOrder;

    @JsonProperty("online_order")
    private boolean onlineOrder;

    @JsonProperty("count_left")
    private double countLeft;

    public static Product toProduct(ProductDto productDto) {
        Product product = new Product(productDto.id, productDto.title);
        product.setCountLeft((int) Math.round(productDto.countLeft));
        product.setPrice(productDto.price);
        return product;
    }
}
