package uz.telegram.bots.orderbot.bot.dto;

import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.user.Category;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class CategoryDto {
    private String title;
    private List<ProductDto> courses;

    public static Category toCategory(CategoryDto categoryDto) {
        Category category = new Category(categoryDto.title);
        category.addAllProducts(categoryDto.getCourses()
                .stream()
                .map(ProductDto::toProduct)
                .peek(p -> p.setCategory(category))
                .collect(Collectors.toList()));
        return category;

    }
}
