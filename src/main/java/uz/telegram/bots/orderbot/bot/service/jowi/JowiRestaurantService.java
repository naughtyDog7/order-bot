package uz.telegram.bots.orderbot.bot.service.jowi;

import uz.telegram.bots.orderbot.bot.user.Restaurant;

import java.io.IOException;
import java.util.List;

interface JowiRestaurantService {
    List<Restaurant> fetchAndUpdateRestaurants() throws IOException;
}
