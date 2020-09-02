package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.telegram.bots.orderbot.bot.repository.TelegramUserRepository;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Optional;

@Service
public class TelegramUserServiceImpl implements TelegramUserService {

    private final TelegramUserRepository repo;

    public Optional<TelegramUser> findById(Integer integer) {
        return repo.findById(integer);
    }

    @Autowired
    public TelegramUserServiceImpl(TelegramUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<TelegramUser> getCommentReceivers() {
        return repo.findAllByReceiveCommentsTrue();
    }

    @Override
    public synchronized TelegramUser getOrSaveUser(Update update) {
        User user = null;
        long chatId = -1;
        if (update.hasMessage()) {
            user = update.getMessage().getFrom();
            chatId = update.getMessage().getChatId();
        } else if(update.hasCallbackQuery()) {
            user = update.getCallbackQuery().getFrom();
        } else if (update.hasPreCheckoutQuery()) {
            user = update.getPreCheckoutQuery().getFrom();
        } else {
            throw new NullPointerException("Couldn't get user from update");
        }
        Optional<TelegramUser> optTelegramUser = repo.findByUserId(user.getId());
        TelegramUser telegramUser;
        if (optTelegramUser.isEmpty()) {
            telegramUser = repo.save(new TelegramUser(user.getId(), chatId,
                    user.getUserName(), user.getFirstName(), user.getLastName()));
        } else {
            telegramUser = optTelegramUser.get();
        }
        return telegramUser;
    }

    @Override
    public <S extends TelegramUser> S save(S s) {
        return repo.save(s);
    }

    @Override
    public TelegramUser getByOrderId(long id) {
        return repo.getByOrderId(id);
    }
}
