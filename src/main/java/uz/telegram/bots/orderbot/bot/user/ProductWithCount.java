package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class ProductWithCount {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private int count;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    private final Order order;

    public static ProductWithCount fromOrder(Order order, Product product, int count) {
        ProductWithCount productWithCount = new ProductWithCount(order);
        productWithCount.setProduct(product);
        productWithCount.setCount(count);
        return productWithCount;
    }
}
