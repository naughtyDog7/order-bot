package uz.telegram.bots.orderbot.bot.service;

import uz.telegram.bots.orderbot.bot.user.Location;

public interface LocationService {
    <S extends Location> S save(S s);
}
