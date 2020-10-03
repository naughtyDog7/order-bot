package uz.telegram.bots.orderbot.bot.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.telegram.telegrambots.ApiContextInitializer;
import uz.telegram.bots.orderbot.OrderBotApplication;
import uz.telegram.bots.orderbot.bot.dto.WebhookOrderWrapperDto;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = OrderBotApplication.class)
@TestInstance(PER_CLASS)
@ActiveProfiles("test")
class WebhookControllerTest {

    static {
        ApiContextInitializer.init();
    }

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mvc;
    private ObjectMapper objectMapper;

    @BeforeAll
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).build();
        objectMapper = new ObjectMapper();
    }


    @Test
    void proceedWebhook() throws Exception {
        WebhookOrderWrapperDto orderWrapperDto = new WebhookOrderWrapperDto();
        orderWrapperDto.setStatus(-1);
        mvc.perform(post("/webhook/").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderWrapperDto)))
                .andExpect(status().isBadRequest());
    }
}