package uz.telegram.bots.orderbot.bot.handler.message.state;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.telegram.bots.orderbot.bot.user.TelegramUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;


@Component
public class MessageStateFactory {

    private final Map<Class<? extends MessageState>, MessageState> messageStatesMap;

    public MessageStateFactory(List<MessageState> messageStatesList) {
        messageStatesMap = messageStatesList.stream().collect(toMap(MessageState::getClass, ms -> ms));
    }

    public Optional<MessageState> getMessageState(Update update, TelegramUser telegramUser) {
        MessageState ms;
        switch (telegramUser.getCurState()) {
            case PRE_GREETING:
                ms = messageStatesMap.get(PreGreetingMessageState.class);
                break;
            case FIRST_LANGUAGE_CONFIGURE:
                ms = messageStatesMap.get(FirstLanguageConfigurationMessageState.class);
                break;
            case MAIN_MENU:
                ms = messageStatesMap.get(MainMenuMessageState.class);
                break;
            case SETTINGS:
                ms = messageStatesMap.get(SettingsMessageState.class);
                break;
            case LANGUAGE_CONFIGURE:
                ms = messageStatesMap.get(LanguageConfigurationMessageState.class);
                break;
            case CONTACT_US:
                ms = messageStatesMap.get(ContactUsMessageState.class);
                break;
            case ORDER_MAIN:
                ms = messageStatesMap.get(OrderMainMessageState.class);
                break;
            case CATEGORY_MAIN:
                ms = messageStatesMap.get(CategoryMainMessageState.class);
                break;
            case PRODUCT_NUM_CHOOSE:
                ms = messageStatesMap.get(ProductNumChooseState.class);
                break;
            case BASKET_MAIN:
                ms = messageStatesMap.get(BasketMainMessageState.class);
                break;
            case SETTINGS_PHONE_NUM:
                ms = messageStatesMap.get(SettingsPhoneNumState.class);
                break;
            case ORDER_PHONE_NUM:
                ms = messageStatesMap.get(OrderPhoneNumState.class);
                break;
            case LOCATION_SENDING:
                ms = messageStatesMap.get(LocationSendState.class);
                break;
            case PAYMENT_METHOD_CHOOSE:
                ms = messageStatesMap.get(PaymentMethodMessageState.class);
                break;
            case FINAL_CONFIRMATION:
                ms = messageStatesMap.get(FinalConfirmationMessageState.class);
                break;
            case WAITING_ORDER_CONFIRM:
                ms = messageStatesMap.get(WaitingOrderConfirmMessageState.class);
                break;
            case ONLINE_PAYMENT:
                ms = messageStatesMap.get(OnlinePaymentMessageState.class);
                break;
            default:
                ms = null;
        }
        return Optional.ofNullable(ms);
    }
}
