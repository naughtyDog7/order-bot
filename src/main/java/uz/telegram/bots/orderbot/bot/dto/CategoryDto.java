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

    public static Category getNewOrUpdateOld(CategoryDto categoryDto, Category oldCategory, ProductRepository productRepository) {
        Category category;
        if (oldCategory != null) {
            category = oldCategory;
        } else {
            category = new Category();
            category.setProducts(categoryDto.getCourses()
                    .stream()
                    .map(p -> ProductDto.getNewOrUpdateOld(p, productRepository.getByProductId(p.getId()).orElse(null)))
                    .peek(p -> p.setCategory(category))
                    .collect(Collectors.toList()));
        }
        category.setName(categoryDto.getTitle());
        return category;
    }
}
