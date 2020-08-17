package uz.telegram.bots.orderbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class OrderBotApplication {

    public static void main(String[] args) {
        ApiContextInitializer.init();
        SpringApplication.run(OrderBotApplication.class, args);
    }

}
