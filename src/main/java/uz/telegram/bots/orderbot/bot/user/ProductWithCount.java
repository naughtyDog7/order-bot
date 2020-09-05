package uz.telegram.bots.orderbot.bot.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.StringJoiner;

@Entity
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@Getter
@Setter
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

    @Override
    public String toString() {
        return new StringJoiner(", ", ProductWithCount.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("count=" + count)
                .add("product=" + product.getName())
                .add("orderId=" + (order == null ? -1 : order.getId()))
                .toString();
    }
}
