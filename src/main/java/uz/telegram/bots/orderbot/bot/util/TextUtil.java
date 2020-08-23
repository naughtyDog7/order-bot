package uz.telegram.bots.orderbot.bot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uz.telegram.bots.orderbot.bot.service.ProductService;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

import java.text.DecimalFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class can be used to create text structures
 * Should only be used in resource lock
 */
@Component
public class TextUtil {

    private final ProductService productService;

    @Autowired
    public TextUtil(ProductService productService) {
        this.productService = productService;
    }



    public static final List<String> MEAL_EMOJIS = List.of("\uD83E\uDD57"/*🥗*/, "\uD83E\uDD58"/*🥘*/, "\uD83C\uDF5C"/*🍜*/,
            "\uD83C\uDF5D"/*🍝*/, "\uD83C\uDF72"/*🍲*/, "\uD83C\uDF5B"/*🍛*/, "\uD83C\uDF71"/*🍱*/, "\uD83E\uDD5F"/*🥟*/, "\uD83C\uDF5A" /*🍚*/,
            "\uD83E\uDD59"/*🥙*/, "\uD83C\uDF75"/*🍵*/, "☕"/*☕*/);
    public static String getRandMealEmoji() {
        return MEAL_EMOJIS.get(ThreadLocalRandom.current().nextInt(MEAL_EMOJIS.size()));
    }

    public String appendProducts(List<ProductWithCount> products, ResourceBundle rb) {
        StringBuilder result = new StringBuilder();
        double totalSum = 0.0;
        for (ProductWithCount productWithCount : products) {
            Product product = productService.fromProductWithCount(productWithCount.getId());
            String appendToCount = rb.getString("count-append");
            double priceForProduct = product.getPrice() * productWithCount.getCount();
            totalSum += priceForProduct;
            result.append("\n")
                    .append(product.getName())
                    .append(" ➡ ")
                    .append(DECIMAL_FORMAT.format(product.getPrice()))
                    .append(" * ")
                    .append(productWithCount.getCount())
                    .append(appendToCount)
                    .append(" = ")
                    .append(DECIMAL_FORMAT.format(priceForProduct))
                    .append(" ")
                    .append(rb.getString("uzs-text"));
        }
        result.append("\n\nTOTAL ==> ").append(DECIMAL_FORMAT.format(totalSum)).append(" ").append(rb.getString("uzs-text"));
        return result.toString();
    }

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");
}
