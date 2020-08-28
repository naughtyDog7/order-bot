package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.TelegramLocation;

public interface LocationService {
    <S extends TelegramLocation> S save(S s);
    TelegramLocation findByPaymentInfoId(long id);
}
