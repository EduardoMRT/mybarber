package br.com.mybarber.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Bean
    public Clock relogio(MyBarberProperties props) {
        return Clock.system(ZoneId.of(props.zona()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }
}
