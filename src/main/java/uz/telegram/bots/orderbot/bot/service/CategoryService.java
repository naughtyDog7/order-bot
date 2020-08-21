package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Category;

import java.util.List;

public interface CategoryService {
    List<Category> updateAndFetchCategories(String restaurantId);
}
