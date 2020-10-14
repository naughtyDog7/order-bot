package uz.telegram.bots.orderbot.bot.handler.message.state;

public enum UserState {
    PRE_GREETING(PreGreetingMessageState.class),
    FIRST_LANGUAGE_CONFIGURE(FirstLanguageConfigurationMessageState.class),
    MAIN_MENU(MainMenuMessageState.class),
    SETTINGS(SettingsMessageState.class),
    LANGUAGE_CONFIGURE(LanguageConfigurationMessageState.class),
    RESTAURANT_CHOOSE(RestaurantChooseMessageState.class),
    ORDER_MAIN(OrderMainMessageState.class),
    CONTACT_US(ContactUsMessageState.class),
    CATEGORY_MAIN(CategoryMainMessageState.class),
    PRODUCT_NUM_CHOOSE(ProductNumChooseState.class),
    BASKET_MAIN(BasketMainMessageState.class),
    SETTINGS_PHONE_NUM(SettingsPhoneNumState.class),
    ORDER_PHONE_NUM(OrderPhoneNumState.class),
    PAYMENT_METHOD_CHOOSE(PaymentMethodMessageState.class),
    FINAL_CONFIRMATION(FinalConfirmationMessageState.class),
    WAITING_ORDER_CONFIRM(WaitingOrderConfirmMessageState.class),
    ONLINE_PAYMENT(OnlinePaymentMessageState.class),
    LOCATION_SENDING(LocationSendState.class);

    private final Class<? extends MessageState> stateHandlerClass;

    UserState(Class<? extends MessageState> stateHandlerClass) {
        this.stateHandlerClass = stateHandlerClass;
    }

    public Class<? extends MessageState> getStateHandlerClass() {
        return stateHandlerClass;
    }
}
