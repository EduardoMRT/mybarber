package br.com.mybarber.api.seguranca;

import br.com.mybarber.api.dominio.Papel;
import br.com.mybarber.api.erro.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthContexto {

    public UsuarioLogado atual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Entre na sua conta para continuar.");
        }
        return new UsuarioLogado(
                Long.valueOf(jwt.getSubject()),
                Papel.valueOf(jwt.getClaimAsString("papel")),
                numero(jwt.getClaim("barbeariaId")),
                numero(jwt.getClaim("barbeiroId")));
    }

    /** Barbearia do usuario do painel (dono ou barbeiro). */
    public Long barbeariaId() {
        Long id = atual().barbeariaId();
        if (id == null) {
            throw ApiException.proibido("Usuário sem barbearia vinculada.");
        }
        return id;
    }

    private static Long numero(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number n) return n.longValue();
        return Long.valueOf(valor.toString());
    }
}
