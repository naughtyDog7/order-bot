package uz.telegram.bots.orderbot.bot.user;

import javax.persistence.*;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderU orderU;

    @ManyToOne
    private Category category;
}
