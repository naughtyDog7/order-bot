package uz.telegram.bots.orderbot.bot.util;

import java.net.URI;

public interface UriUtil {
    URI getRestaurantsGetUri();

    URI getMenuGetUri(String restaurantId);

    URI getOrderPostUri();

    URI getOrderGetUri(String orderStringId);

    URI getOrderCancelUri(String orderStringId);
}
