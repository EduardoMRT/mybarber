package br.com.mybarber.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class MyBarberApplication {

    public static void main(String[] args) {
        // RNF07: todos os horarios no fuso de Brasilia, independente do servidor.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(MyBarberApplication.class, args);
    }
}
