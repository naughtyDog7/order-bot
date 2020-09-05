package uz.telegram.bots.orderbot.bot.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CategoryDto {
    private String title;
    private List<ProductDto> courses;
}
