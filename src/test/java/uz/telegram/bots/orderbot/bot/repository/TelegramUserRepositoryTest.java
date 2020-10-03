package uz.telegram.bots.orderbot.bot.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uz.telegram.bots.orderbot.bot.user.Order;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
class TelegramUserRepositoryTest {

    @Autowired
    private TelegramUserRepository repo;
    @Autowired
    private OrderRepository orderRepo;

    private static int idCounter = 0;

    @Test
    void findByUserId() {
        TelegramUser telegramUser = getTestUser();
        telegramUser = repo.save(telegramUser);
        assertEquals(repo.findByUserId(telegramUser.getUserId()).orElseThrow(), telegramUser);
    }

    @Test
    void findAllByReceiveCommentsTrue() {
        TelegramUser commentReceiver1 = getTestUser();
        commentReceiver1.setReceiveComments(true);
        commentReceiver1 = repo.save(commentReceiver1);

        TelegramUser commentReceiver2 = getTestUser();
        commentReceiver2.setReceiveComments(true);
        commentReceiver2 = repo.save(commentReceiver2);

        TelegramUser notCommentReceiver = getTestUser();
        notCommentReceiver = repo.save(notCommentReceiver);

        List<TelegramUser> commentReceivers = repo.findAllByReceiveCommentsTrue();
        assertThat(commentReceivers, containsInAnyOrder(commentReceiver1, commentReceiver2));
        assertThat(commentReceivers, not(contains(notCommentReceiver)));
    }

    @Test
    void findByOrderId() {
        TelegramUser testUser = getTestUser();
        testUser = repo.save(testUser);
        Order order = new Order(testUser);
        order = orderRepo.save(order);
        assertEquals(testUser, repo.findByOrderId(order.getId()));
    }

    private TelegramUser getTestUser() {
        return new TelegramUser(idCounter++, 1200 + idCounter, "username" + idCounter,
                "name" + idCounter, "surname" + idCounter);
    }
}
