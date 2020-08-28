package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.PaymentInfo;

public interface PaymentInfoService {
    <S extends PaymentInfo> S save(S s);
    PaymentInfo getFromOrderId(long orderId);
}
