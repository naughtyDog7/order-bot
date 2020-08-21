package uz.telegram.bots.orderbot.bot.config;

import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;
import java.util.Set;

@Configuration
public class JsonPathConfiguration {
    @Autowired
    public void configureJsonPath() {
        com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {
            @Override
            public JsonProvider jsonProvider() {
                return new JacksonJsonProvider();
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }

            @Override
            public MappingProvider mappingProvider() {
                return new JacksonMappingProvider();
            }
        });
    }
}
