package uz.telegram.bots.orderbot.bot.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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



    @OneToMany(mappedBy = "telegramUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final List<Order> orders = new ArrayList<>();


    private Role role = Role.USER;

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

    public enum UserState {
        PRE_GREETING,
        FIRST_LANGUAGE_CONFIGURE,
        MAIN_MENU,
        SETTINGS,
        LANGUAGE_CONFIGURE,
        ORDER_MAIN,
        CONTACT_US
    }

    public enum Role {
        USER
    }



}
