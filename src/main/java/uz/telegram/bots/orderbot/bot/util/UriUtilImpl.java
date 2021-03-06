package uz.telegram.bots.orderbot.bot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;

import java.net.URI;

@Component
public class UriUtilImpl implements UriUtil {
    private final URI restaurantsUri;
    private final UriComponents menuTemplate;
    private final URI orderPostUri;
    private final UriComponents orderGetUri;
    private final UriComponents orderCancelUri;

    @Autowired
    public UriUtilImpl(JowiProperties jowiProperties) {
        restaurantsUri = UriComponentsBuilder.fromUriString(jowiProperties.getApiUrlV010())
                .path("/restaurants")
                .query("api_key=" + jowiProperties.getApiKey())
                .query("sig=" + jowiProperties.getSig())
                .build().toUri();
        menuTemplate = UriComponentsBuilder.fromUri(restaurantsUri)
                .path("/{restaurant-id}")
                .build();
        orderPostUri = UriComponentsBuilder.fromUriString(jowiProperties.getApiUrlV3())
                .path("/orders")
                .build().toUri();
        orderGetUri = UriComponentsBuilder.fromUriString(jowiProperties.getApiUrlV3())
                .path("/orders/{order-id}")
                .build();
        orderCancelUri = UriComponentsBuilder.fromUriString(jowiProperties.getApiUrlV3())
                .path("/orders/{order-id}/cancel")
                .build();
    }

    @Override
    public URI getRestaurantsGetUri() {
        return restaurantsUri;
    }

    @Override
    public URI getMenuGetUri(String restaurantId) {
        return menuTemplate.expand(restaurantId).toUri();
    }

    @Override
    public URI getOrderPostUri() {
        return orderPostUri;
    }

    @Override
    public URI getOrderGetUri(String orderStringId) {
        return orderGetUri.expand(orderStringId).toUri();
    }

    @Override
    public URI getOrderCancelUri(String orderStringId) {
        return orderCancelUri.expand(orderStringId).toUri();
    }
}
