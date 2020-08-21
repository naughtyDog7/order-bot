package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(unique = true, nullable = false)
    private final String restaurantId;
    private final String restaurantTitle;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Location location;

    private boolean onlineOrder;
    private double deliveryPrice;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final List<Category> categories = new ArrayList<>();
}
