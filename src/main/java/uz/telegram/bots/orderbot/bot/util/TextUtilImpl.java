package uz.telegram.bots.orderbot.bot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uz.telegram.bots.orderbot.bot.service.ProductService;
import uz.telegram.bots.orderbot.bot.service.RestaurantService;
import uz.telegram.bots.orderbot.bot.user.PaymentInfo;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;
import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * This class can be used to create text structures
 * Should only be used in resource lock
 */
@Component
public class TextUtilImpl implements TextUtil {

    private final ProductService productService;
    private final RestaurantService restaurantService;

    private static final ZoneId TASHKENT_ZONE_ID = ZoneId.of("GMT+5");

    @Autowired
    public TextUtilImpl(ProductService productService, RestaurantService restaurantService) {
        this.productService = productService;
        this.restaurantService = restaurantService;
    }

    @Override
    public StringBuilder appendProducts(StringBuilder initial, List<ProductWithCount> products, ResourceBundle rb, boolean withDelivery, int deliveryPrice) {
        long totalSum = 0L;
        for (ProductWithCount productWithCount : products) {
            Product product = productService.fromProductWithCount(productWithCount.getId());
            String appendToCount = rb.getString("count-append");
            long priceForProduct = product.getPrice() * (long) productWithCount.getCount();
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
        if (withDelivery) {
            appendDeliveryPrice(initial, rb, deliveryPrice);
            totalSum += deliveryPrice;
        }
        initial.append("\n\n")
                .append(rb.getString("total"))
                .append(": ==> ")
                .append(totalSum)
                .append(" ")
                .append(rb.getString("uzs-text"));
        return initial;
    }

    private void appendDeliveryPrice(StringBuilder initial, ResourceBundle rb, int deliveryPrice) {
        initial.append("\n")
                .append(rb.getString("delivery"))
                .append(": ");
        if (deliveryPrice == 0)
            initial.append(rb.getString("free"));
        else
            initial.append(deliveryPrice)
                    .append(" ")
                    .append(rb.getString("uzs-text"));
    }

    @Override
    public void appendPhoneNum(StringBuilder initial, String phoneNum, ResourceBundle rb) {
        initial
                .append("\n\n")
                .append(rb.getString("phone-number"))
                .append(": ")
                .append(phoneNum);
    }

    @Override
    public void appendNoNameLocation(StringBuilder initial, ResourceBundle rb) {
        initial.append("\n")
                .append(rb.getString("location"))
                .append(": ")
                .append(rb.getString("chosen-location"));
    }

    @Override
    public void appendPaymentMethod(StringBuilder initial, PaymentInfo.PaymentMethod paymentMethod, ResourceBundle rb) {
        initial.append("\n")
                .append(rb.getString("payment-method"))
                .append(": ")
                .append(rb.getString(paymentMethod.getRbValue()));
    }


    @Override
    public void appendRestaurants(StringBuilder text, List<Restaurant> restaurants, ResourceBundle rb) {
        List<Restaurant> restaurantsCopy = new ArrayList<>(restaurants);
        LocalDateTime curTime = LocalDateTime.now(TASHKENT_ZONE_ID);
        sortRestaurantsOpenedFirst(restaurantsCopy, curTime);
        appendSortedRestaurants(text, rb, restaurantsCopy, curTime);
    }

    private void sortRestaurantsOpenedFirst(List<Restaurant> restaurantsCopy, LocalDateTime curTime) {
        restaurantsCopy.sort(((Comparator<Restaurant>) (f, s) ->
                Boolean.compare(restaurantService.isOpened(curTime, f), restaurantService.isOpened(curTime, s))).reversed());
    }

    private void appendSortedRestaurants(StringBuilder text, ResourceBundle rb, List<Restaurant> restaurantsCopy, LocalDateTime curTime) {
        for (int i = 0; i < restaurantsCopy.size(); i++) {
            Restaurant restaurant = restaurantsCopy.get(i);
            text.append(i + 1).append(") ")
                    .append(restaurant.getRestaurantTitle());

            appendRestaurantAddress(text, rb, restaurant);
            if (!restaurantService.isOpened(curTime, restaurant)) {
                text.append("\n").append(rb.getString("restaurant-currently-closed"));
            }
            text.append("\n\n");
        }
    }

    private void appendRestaurantAddress(StringBuilder text, ResourceBundle rb, Restaurant restaurant) {
        String address = restaurant.getAddress();
        if (address != null) {
            text.append("\n").append(rb.getString("address"))
                    .append(": ")
                    .append(address);
        }
    }
}
