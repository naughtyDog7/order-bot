package uz.telegram.bots.orderbot.bot.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor(force = true)
@Getter
@Setter
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "category", cascade = {CascadeType.MERGE, CascadeType.REMOVE},
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant;

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", products=" + Product.productNames(products) +
                ", restaurant=" + restaurant.getRestaurantTitle() +
                '}';
    }

    public static List<String> getNames(List<Category> categories) {
        return categories.stream().map(Category::getName).collect(Collectors.toList());
    }
}
