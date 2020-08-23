package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

import java.util.List;
import java.util.Optional;

public interface ProductWithCountService {
    int getBasketItemsCount(long orderId);

    ProductWithCount save(ProductWithCount productWithCount);

    Optional<ProductWithCount> findByOrderIdAndProductId(long orderId, long productId);

    List<ProductWithCount> getAllFromOrderId(long orderId);

    Optional<ProductWithCount> getByOrderIdAndProductName(long orderId, String productName);

    void deleteById(Long aLong);
}
