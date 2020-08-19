package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@Entity
@RequiredArgsConstructor
@NoArgsConstructor
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private final String restaurantId;
    private final String restaurantTitle;
    private final double longitude;
    private final double latitude;
    private boolean onlineOrder;
    private double deliveryPrice;

    @ManyToMany(mappedBy = "restaurants", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private final List<Category> categories = new ArrayList<>();
}
