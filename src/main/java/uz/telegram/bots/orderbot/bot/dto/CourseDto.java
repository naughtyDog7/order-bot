package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import uz.telegram.bots.orderbot.bot.service.ProductService;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

@Getter
@Setter
@ToString
public class CourseDto {
    private int count;
    @JsonProperty("course_id")
    private String courseId;

    public CourseDto(int count, String courseId) {
        this.count = count;
        this.courseId = courseId;
    }

    public static CourseDto fromProductWithCount(ProductWithCount productWithCount, ProductService productService) {
        return new CourseDto(productWithCount.getCount(), productService.fromProductWithCount(productWithCount.getId()).getProductId());
    }
}
