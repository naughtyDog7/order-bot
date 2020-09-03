package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.user.Product;

import java.util.Objects;

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

    public static Product getNewOrUpdateOld(ProductDto productDto, Product oldProduct, ProductRepository productRepository) {
        Product product;
        product = Objects.requireNonNullElseGet(oldProduct, Product::new);
        product.setProductId(productDto.id);
        product.setName(productDto.title);
        product.setCountLeft((int) Math.round(productDto.countLeft));
        product.setPrice((int) Math.round(productDto.price));
        if (product.getId() == 0) {
            product = productRepository.save(product);
        }
        return product;
    }
}
