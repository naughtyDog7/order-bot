package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Entity
@NoArgsConstructor(force = true)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter
    private int id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant;

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Product> getProducts() {
        return products;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", products=" + Product.productNames(products) +
                ", restaurant=" + restaurant.getRestaurantTitle() +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static List<String> getNames(List<Category> categories) {
        return categories.stream().map(Category::getName).collect(Collectors.toList());
    }


}
