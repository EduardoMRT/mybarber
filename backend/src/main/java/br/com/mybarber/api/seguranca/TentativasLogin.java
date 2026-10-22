package br.com.mybarber.api.seguranca;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Protecao simples contra forca bruta: 5 erros seguidos bloqueiam o e-mail por 5 minutos. */
@Component
public class TentativasLogin {

    private static final int MAXIMO = 5;
    private static final Duration ESPERA = Duration.ofMinutes(5);

    private record Registro(int falhas, Instant ultima) {
    }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public boolean bloqueado(String email) {
        Registro r = registros.get(chave(email));
        if (r == null) return false;
        if (r.ultima().plus(ESPERA).isBefore(Instant.now())) {
            registros.remove(chave(email));
            return false;
        }
        return r.falhas() >= MAXIMO;
    }

    public void falhou(String email) {
        registros.merge(chave(email), new Registro(1, Instant.now()),
                (antigo, novo) -> new Registro(antigo.falhas() + 1, Instant.now()));
    }

    public void sucesso(String email) {
        registros.remove(chave(email));
    }

    private static String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
