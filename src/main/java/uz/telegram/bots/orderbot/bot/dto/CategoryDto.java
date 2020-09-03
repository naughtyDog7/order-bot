package uz.telegram.bots.orderbot.bot.dto;

import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.repository.ProductRepository;
import uz.telegram.bots.orderbot.bot.user.Category;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class CategoryDto {
    private String title;
    private List<ProductDto> courses;

    public static Category toCategory(CategoryDto categoryDto, Category oldCategory, ProductRepository productRepository) {
        Category category = new Category(categoryDto.title);
        category.addAllProducts(categoryDto.getCourses()
                .stream()
                .map(p -> ProductDto.toProduct(p, productRepository.getByProductId(p.getId()).orElse(null)))
                .peek(p -> p.setCategory(category))
                .collect(Collectors.toList()));
        if (oldCategory != null) {
            category.setId(oldCategory.getId());
        }
        return category;

    }
}
