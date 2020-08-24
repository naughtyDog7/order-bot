package uz.telegram.bots.orderbot.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

@Getter
@Setter
public class CourseDto {
    private int count;
    @JsonProperty("course_id")
    private String courseId;

    public CourseDto(int count, String courseId) {
        this.count = count;
        this.courseId = courseId;
    }

    //pass only eagerly initialized productwithcount
    public static CourseDto fromProductWithCount(ProductWithCount productWithCount) {
        return new CourseDto(productWithCount.getCount(), productWithCount.getProduct().getProductId());
    }
}
