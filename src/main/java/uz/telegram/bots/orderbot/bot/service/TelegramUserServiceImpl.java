package uz.telegram.bots.orderbot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.telegram.bots.orderbot.bot.repository.TelegramUserRepository;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public List<TelegramUser> findCommentReceivers() {
        return repo.findAllByReceiveCommentsTrue();
    }

    @Override
    public synchronized TelegramUser getOrSaveAndGetUserFromUpdate(Update update) {
        User user;
        long chatId = -1;
        if (update.hasMessage()) {
            user = update.getMessage().getFrom();
            chatId = update.getMessage().getChatId();
        } else if(update.hasCallbackQuery()) {
            user = update.getCallbackQuery().getFrom();
        } else if (update.hasPreCheckoutQuery()) {
            user = update.getPreCheckoutQuery().getFrom();
        } else {
            throw new IllegalArgumentException("Couldn't get user from update");
        }
        return getOrSaveAndGetUser(user, chatId);
    }

    private TelegramUser getOrSaveAndGetUser(User user, long chatId) {
        Optional<TelegramUser> optTelegramUser = repo.findByUserId(user.getId());
        if (optTelegramUser.isEmpty()) {
            return repo.save(new TelegramUser(user.getId(), chatId,
                    user.getUserName(), user.getFirstName(), user.getLastName()));
        } else {
            return optTelegramUser.get();
        }
    }

    @Override
    public <S extends TelegramUser> S save(S s) {
        return repo.save(s);
    }

    @Override
    public TelegramUser findByOrderId(long id) {
        return repo.findByOrderId(id);
    }

    private static final Pattern PHONE_NUM_PATTERN = Pattern.compile("^(?:\\+?998)?[ -]?(\\d{2})[ -]?(\\d{3})[ -]?(\\d{2})[ -]?(\\d{2})$");

    @Override
    public void checkAndSetPhoneNum(TelegramUser telegramUser, String phoneNum) {
        Matcher m = PHONE_NUM_PATTERN.matcher(phoneNum);
        String cleanedPhoneNum;
        if (m.matches()) {
            cleanedPhoneNum = m.group(1) + "-" + m.group(2) + "-" + m.group(3) + "-" + m.group(4);
        } else {
            throw new IllegalArgumentException("Phone number doesn't match predefined pattern. Number: " + phoneNum + ", pattern " + PHONE_NUM_PATTERN.pattern());
        }
        telegramUser.setPhoneNum(cleanedPhoneNum);
    }
}
