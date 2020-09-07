package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.ProductWithCountRepository;
import uz.telegram.bots.orderbot.bot.user.ProductWithCount;

import java.util.List;
import java.util.Optional;

@Service
public class ProductWithCountServiceImpl implements ProductWithCountService {

    private final ProductWithCountRepository repository;

    @Autowired
    public ProductWithCountServiceImpl(ProductWithCountRepository repository) {
        this.repository = repository;
    }

    @Override
    public int getBasketItemsCount(long orderId) {
        return repository.countAllByOrderId(orderId);
    }

    @Override
    public ProductWithCount save(ProductWithCount productWithCount) {
        return repository.save(productWithCount);
    }

    @Override
    public Optional<ProductWithCount> findByOrderIdAndProductId(long orderId, long productId) {
        return repository.findByOrderIdAndProductId(orderId, productId);
    }

    @Override
    public Optional<ProductWithCount> findByOrderIdAndProductName(long orderId, String productName) {
        return repository.findByOrderIdAndProductName(orderId, productName);
    }

    @Override
    public List<ProductWithCount> findByOrderId(long orderId) {
        return repository.findByOrderId(orderId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
