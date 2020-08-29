package uz.telegram.bots.orderbot.bot.service;


import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Optional;

public interface TelegramUserService {
    Optional<TelegramUser> findById(Integer integer);
    List<TelegramUser> getCommentReceivers();
    TelegramUser getOrSaveUser(Update update);
    <S extends TelegramUser> S save(S s);

    TelegramUser getByOrderId(long id);
}
