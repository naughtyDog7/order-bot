package uz.telegram.bots.orderbot.bot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uz.telegram.bots.orderbot.bot.service.ProductService;
import uz.telegram.bots.orderbot.bot.user.PaymentInfo;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

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

    public StringBuilder appendProducts(StringBuilder initial, List<ProductWithCount> products, ResourceBundle rb) {
        long totalSum = 0L;
        for (ProductWithCount productWithCount : products) {
            Product product = productService.fromProductWithCount(productWithCount.getId());
            String appendToCount = rb.getString("count-append");
            long priceForProduct = product.getPrice() * (long)productWithCount.getCount();
            totalSum += priceForProduct;
            initial.append("\n")
                    .append(product.getName())
                    .append(" ➡ ")
                    .append(product.getPrice())
                    .append(" * ")
                    .append(productWithCount.getCount())
                    .append(appendToCount)
                    .append(" = ")
                    .append(priceForProduct)
                    .append(" ")
                    .append(rb.getString("uzs-text"));
        }
        initial.append("\n\n")
                .append(rb.getString("total"))
                .append(": ==> ")
                .append(totalSum)
                .append(" ")
                .append(rb.getString("uzs-text"));
        return initial;
    }

    public StringBuilder appendPhoneNum(StringBuilder initial, String phoneNum, ResourceBundle rb) {
        return initial
                .append("\n\n")
                .append(rb.getString("phone-number"))
                .append(": ")
                .append(phoneNum);
    }

    public StringBuilder appendNoNameLocation(StringBuilder initial, ResourceBundle rb) {
        return initial.append("\n")
                .append(rb.getString("location"))
                .append(": ")
                .append(rb.getString("chosen-location"));
    }

    public StringBuilder appendPaymentMethod(StringBuilder initial, PaymentInfo.PaymentMethod paymentMethod, ResourceBundle rb) {
        return initial.append("\n")
                .append(rb.getString("payment-method"))
                .append(": ")
                .append(rb.getString(paymentMethod.getRbValue()));
    }
}
