package uz.telegram.bots.orderbot.bot.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@TestConfiguration
@EnableJpaRepositories(basePackages = "uz.telegram.bots.orderbot.bot.repository")
@EntityScan(basePackages = "uz.telegram.bots.orderbot.bot.user")
@AutoConfigureDataJpa
@ActiveProfiles("test")
public class TestJpaConfig {
}
