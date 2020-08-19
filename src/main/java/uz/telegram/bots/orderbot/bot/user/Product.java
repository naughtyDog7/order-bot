package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private final String productId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderU orderU;

    @ManyToOne
    private Category category;
}
