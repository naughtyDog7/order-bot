package uz.telegram.bots.orderbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.telegram.telegrambots.ApiContextInitializer;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class OrderBotApplicationTests {

	static {
		ApiContextInitializer.init();
	}

	@Test
	void contextLoads() {
	}

}
