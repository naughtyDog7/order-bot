package uz.telegram.bots.orderbot.bot.util;

import uz.telegram.bots.orderbot.bot.user.PaymentInfo;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;
import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;

public interface TextUtil {
    StringBuilder appendProducts(StringBuilder initial, List<ProductWithCount> products, ResourceBundle rb, boolean withDelivery, int deliveryPrice);

    void appendPhoneNum(StringBuilder initial, String phoneNum, ResourceBundle rb);

    void appendNoNameLocation(StringBuilder initial, ResourceBundle rb);

    void appendPaymentMethod(StringBuilder initial, PaymentInfo.PaymentMethod paymentMethod, ResourceBundle rb);

    void appendRestaurants(StringBuilder text, List<Restaurant> restaurants, ResourceBundle rb);

    List<String> MEAL_EMOJIS = List.of("\uD83E\uDD57"/*🥗*/, "\uD83E\uDD58"/*🥘*/, "\uD83C\uDF5C"/*🍜*/,
            "\uD83C\uDF5D"/*🍝*/, "\uD83C\uDF72"/*🍲*/, "\uD83C\uDF5B"/*🍛*/, "\uD83C\uDF71"/*🍱*/, "\uD83E\uDD5F"/*🥟*/, "\uD83C\uDF5A" /*🍚*/,
            "\uD83E\uDD59"/*🥙*/, "\uD83C\uDF75"/*🍵*/, "☕"/*☕*/);

    static String getRandMealEmoji() {
        return MEAL_EMOJIS.get(ThreadLocalRandom.current().nextInt(MEAL_EMOJIS.size()));
    }
}
