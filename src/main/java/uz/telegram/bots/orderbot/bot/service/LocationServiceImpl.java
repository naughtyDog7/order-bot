package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.telegram.bots.orderbot.bot.repository.LocationRepository;
import uz.telegram.bots.orderbot.bot.user.TelegramLocation;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Autowired
    public LocationServiceImpl(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    public <S extends TelegramLocation> S save(S s) {
        return locationRepository.save(s);
    }

    @Override
    public TelegramLocation findByPaymentInfoId(long id) {
        return locationRepository.findByPaymentInfoId(id);
    }
}
