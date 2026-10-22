package br.com.mybarber.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "mybarber")
public record MyBarberProperties(String zona, Jwt jwt, Admin admin, Cors cors, Legal legal) {

    public record Jwt(String segredo, int minutosAcesso, int diasRefresh) {
    }

    public record Admin(String email, String senha, String nome) {
    }

    public record Cors(List<String> origens) {
    }

    public record Legal(String versao) {
    }
}
