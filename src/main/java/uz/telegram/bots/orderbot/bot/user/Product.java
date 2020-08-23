package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Data
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(unique = true, nullable = false)
    private final String productId;

    private final String name;

    private double price;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Category category;

    private volatile int countLeft;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id == product.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static List<String> productNames(List<? extends Product> products) {
        return products.stream().map(Product::getName).collect(Collectors.toList());
    }
}
