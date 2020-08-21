package uz.telegram.bots.orderbot.bot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;

import java.net.URI;

@Component
public class UriUtil {
    private final JowiProperties jowiProperties;
    private final UriComponents restaurantsTemplate;
    private final UriComponents menuTemplate;

    @Autowired
    public UriUtil(JowiProperties jowiProperties) {
        this.jowiProperties = jowiProperties;
        restaurantsTemplate = UriComponentsBuilder.fromUriString(jowiProperties.getApiUrlV010())
                .path("/restaurants")
                .query("api_key=" + jowiProperties.getApiKey())
                .query("sig=" + jowiProperties.getSig())
                .build();
        menuTemplate = UriComponentsBuilder.fromUri(restaurantsTemplate.toUri())
                .path("/{restaurant-id}")
                .build();
    }

    public URI getRestaurantsGetUri() {
        return restaurantsTemplate.toUri();
    }

    public URI getMenuGetUri(String restaurantId) {
        return menuTemplate.expand(restaurantId).toUri();
    }
}
