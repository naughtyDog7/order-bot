package uz.telegram.bots.orderbot.bot.user;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private final String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private final List<Product> products = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private final List<Restaurant> restaurants = new ArrayList<>();

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<Restaurant> getRestaurants() {
        return restaurants;
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
                ", products=" + products +
                ", restaurants=" + restaurants.stream().map(Restaurant::getRestaurantTitle).collect(Collectors.joining()) +
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
