package uz.telegram.bots.orderbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.telegram.bots.orderbot.bot.user.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    //find by restaurant id in restaurant class

    @Query(value = "SELECT c FROM Category c " +
            "JOIN c.restaurant r " +
            "WHERE r.restaurantId = :restaurantId AND " +
            "(SELECT count(p) FROM Product p WHERE p MEMBER OF c.products AND p.countLeft > 0) > 0")
    List<Category> findNonEmptyByRestaurantStringId(String restaurantId);

    Optional<Category> findByNameAndRestaurantId(String name, int restaurantId);

    @Query(value = "SELECT c FROM Order o " +
            "JOIN o.paymentInfo pi " +
            "JOIN pi.fromRestaurant r " +
            "JOIN r.categories c " +
            "WHERE o.id = :orderId")
    List<Category> findByOrderId(long orderId);

    @Query(value = "SELECT c FROM Order o " +
            "JOIN o.paymentInfo pi " +
            "JOIN pi.fromRestaurant r " +
            "JOIN r.categories c " +
            "WHERE o.id = :orderId AND " +
            "(SELECT count(p) FROM Product p WHERE p MEMBER OF c.products AND p.countLeft > 0) > 0")
    List<Category> findAllNonEmptyByOrderId(long orderId);

    List<Category> findAllByRestaurantRestaurantId(String restaurantId);


    Optional<Category> findByNameAndRestaurantRestaurantId(String name, String restaurantId);

    @Query(value = "SELECT c FROM Order o " +
            "JOIN o.paymentInfo pi " +
            "JOIN pi.fromRestaurant r " +
            "JOIN r.categories c " +
            "WHERE o.id = :id AND c.name = o.lastChosenCategoryName")
    Optional<Category> getLastChosenByOrderId(long id);

    @Query("SELECT c FROM Category c " +
            "JOIN FETCH c.products p " +
            "WHERE c.id = :id")
    Optional<Category> getWithProductsById(int id);

    List<Category> deleteAllByRestaurantRestaurantId(String restaurantId);
}
