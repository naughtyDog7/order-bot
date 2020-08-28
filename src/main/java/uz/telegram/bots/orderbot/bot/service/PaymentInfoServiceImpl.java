package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.PaymentInfoRepository;
import uz.telegram.bots.orderbot.bot.user.PaymentInfo;

@Service
public class PaymentInfoServiceImpl implements PaymentInfoService {

    private final PaymentInfoRepository paymentInfoRepository;

    @Autowired
    public PaymentInfoServiceImpl(PaymentInfoRepository paymentInfoRepository) {
        this.paymentInfoRepository = paymentInfoRepository;
    }

    public <S extends PaymentInfo> S save(S s) {
        return paymentInfoRepository.save(s);
    }

    @Override
    public PaymentInfo getFromOrderId(long orderId) {
        return paymentInfoRepository.getByOrderAndId(orderId);
    }
}
