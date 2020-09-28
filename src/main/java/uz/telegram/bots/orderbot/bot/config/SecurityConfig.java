package uz.telegram.bots.orderbot.bot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import uz.telegram.bots.orderbot.bot.properties.SecurityProperties;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final SecurityProperties props;

    @Autowired
    public SecurityConfig(SecurityProperties props) {
        this.props = props;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        auth.inMemoryAuthentication()
                .withUser("user")
                .password(encoder.encode(props.getAdminPass()))
                .roles("ADMIN");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                    .authorizeRequests()
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
                    .anyRequest().permitAll()
                .and()
                    .formLogin()
                .and()
                    .httpBasic()
                .and()
                    .csrf().disable();
    }
}
