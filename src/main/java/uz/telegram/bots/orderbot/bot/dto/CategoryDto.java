package uz.telegram.bots.orderbot.bot.dto;

import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.user.Category;

import java.util.List;

@Getter
@Setter
public class CategoryDto {
    private String title;
    private List<ProductDto> courses;

    public static Category toCategory(CategoryDto categoryDto) {
        return new Category(categoryDto.title);

    }
}
