package uz.telegram.bots.orderbot.bot.service.jowi;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import uz.telegram.bots.orderbot.bot.dto.ProductDto;
import uz.telegram.bots.orderbot.bot.user.Category;
import uz.telegram.bots.orderbot.bot.user.Product;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;

interface JowiProductService {
    void updateProductsForCategories(List<? extends Category> newCategories, String restaurantId);

    Product getNewOrUpdateOldProduct(ProductDto productDto, Product oldProduct);

    void deleteProductsRemovedFromServer(List<Product> oldProducts, List<Product> newProducts,
                                         String restaurantId, TelegramLongPollingBot bot, TelegramUser telegramUser);
}
