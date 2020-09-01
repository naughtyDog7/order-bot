package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class TelegramUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(unique = true, nullable = false)
    private final int userId;
    private final long chatId;

    private final String username;
    private final String firstName;
    private final String lastName;
    private String phoneNum;
    private String langISO = ""; //default lang iso for getting default ResourceBundle from ResourceBundleFactory
    private boolean receiveComments = false;
    private UserState curState = UserState.PRE_GREETING; //default to first User state


    @OneToMany(mappedBy = "telegramUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private final List<Order> orders = new ArrayList<>();

    private Role role = Role.USER;
    private LocalDateTime lastActive;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelegramUser that = (TelegramUser) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    private static final ZoneId tashkentZoneId = ZoneId.of("GMT+5");
    @PrePersist
    public void setLastActive() {
        lastActive = LocalDateTime.now(tashkentZoneId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TelegramUser.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("userId=" + userId)
                .add("chatId=" + chatId)
                .add("username='" + username + "'")
                .add("firstName='" + firstName + "'")
                .add("lastName='" + lastName + "'")
                .add("phoneNum='" + phoneNum + "'")
                .add("langISO='" + langISO + "'")
                .add("receiveComments=" + receiveComments)
                .add("curState=" + curState)
                .add("orders=" + orders
                        .stream()
                        .map(o -> o.getId() + "")
                        .collect(Collectors.joining(",", "[", "]")))
                .add("role=" + role)
                .toString();
    }

    public enum UserState {
        PRE_GREETING,
        FIRST_LANGUAGE_CONFIGURE,
        MAIN_MENU,
        SETTINGS,
        LANGUAGE_CONFIGURE,
        ORDER_MAIN,
        CONTACT_US,
        CATEGORY_MAIN,
        PRODUCT_NUM_CHOOSE,
        BASKET_MAIN,
        SETTINGS_PHONE_NUM,
        ORDER_PHONE_NUM,
        PAYMENT_METHOD_CHOOSE, FINAL_CONFIRMATION, WAITING_ORDER_CONFIRM, ONLINE_PAYMENT, LOCATION_SENDING
    }

    public enum Role {
        USER
    }


}
