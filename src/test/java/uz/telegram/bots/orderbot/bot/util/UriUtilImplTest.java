package uz.telegram.bots.orderbot.bot.util;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uz.telegram.bots.orderbot.bot.properties.JowiProperties;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = UriUtilImpl.class)
@EnableConfigurationProperties(JowiProperties.class)
@TestInstance(PER_CLASS)
class UriUtilImplTest {

    @Autowired
    private UriUtil uriUtil;
    @Autowired
    private JowiProperties jowiProperties;

    @Test
    void getRestaurantsGetUri() throws URISyntaxException {
        URIBuilder expectedBuilder = new URIBuilder(jowiProperties.getApiUrlV010());
        URI expected = expectedBuilder.setPath(expectedBuilder.getPath() + "/restaurants")
                .addParameter("api_key", jowiProperties.getApiKey())
                .addParameter("sig", jowiProperties.getSig())
                .build();
        URI actual = uriUtil.getRestaurantsGetUri();
        assertEquals(expected, actual);
    }

    @Test
    void getMenuGetUri() throws URISyntaxException {
        String restaurantId = "12345678";
        URIBuilder expectedBuilder = new URIBuilder(jowiProperties.getApiUrlV010());
        URI expected = expectedBuilder.setPath(expectedBuilder.getPath() + "/restaurants/" + restaurantId)
                .addParameter("api_key", jowiProperties.getApiKey())
                .addParameter("sig", jowiProperties.getSig())
                .build();
        URI actual = uriUtil.getMenuGetUri(restaurantId);
        assertEquals(expected, actual);
    }
}