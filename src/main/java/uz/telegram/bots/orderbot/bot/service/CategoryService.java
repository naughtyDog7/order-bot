package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.CategoryRepository;
import uz.telegram.bots.orderbot.bot.user.Category;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository repository;

    @Autowired
    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> fetchCategories(String restaurantId) {
        //TODO implement to fetch categories from jowi api
        Category category1 = new Category("Category1");
        Category category2 = new Category("Category2");
        Category category3 = new Category("Category3");
        Category category4 = new Category("Category4");
        Category category5 = new Category("Category5");
        Category category6 = new Category("Category6");
        Category category7 = new Category("Category7");
        return List.of(category1, category2, category3, category4, category5, category6, category7);
    }
}
