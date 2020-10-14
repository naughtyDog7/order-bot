package uz.telegram.bots.orderbot.bot.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uz.telegram.bots.orderbot.bot.handler.message.state.UserState;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Entity
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@Getter
@Setter
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
    @Enumerated(EnumType.STRING)
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
                .add("role=" + role)
                .toString();
    }

    public enum Role {
        USER
    }


}
