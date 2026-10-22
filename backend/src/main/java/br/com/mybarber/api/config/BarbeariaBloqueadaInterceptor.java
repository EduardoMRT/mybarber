package br.com.mybarber.api.config;

import br.com.mybarber.api.repositorio.BarbeariaRepository;
import br.com.mybarber.api.seguranca.AuthContexto;
import br.com.mybarber.api.seguranca.UsuarioLogado;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Barbearia bloqueada (inadimplencia ou decisao do admin) continua entrando no portal para ver
 * a assinatura e consultar dados, mas nao consegue alterar nada (HTTP 423).
 */
@Component
public class BarbeariaBloqueadaInterceptor implements HandlerInterceptor {

    private final BarbeariaRepository barbearias;
    private final AuthContexto contexto;
    private final ObjectMapper json;

    public BarbeariaBloqueadaInterceptor(BarbeariaRepository barbearias, AuthContexto contexto, ObjectMapper json) {
        this.barbearias = barbearias;
        this.contexto = contexto;
        this.json = json;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String metodo = req.getMethod();
        if ("GET".equals(metodo) || "OPTIONS".equals(metodo) || req.getRequestURI().contains("/notificacoes")) {
            return true;
        }
        UsuarioLogado u = contexto.atual();
        if (u.barbeariaId() == null) {
            return true;
        }
        boolean bloqueada = barbearias.findById(u.barbeariaId()).map(b -> b.bloqueada()).orElse(false);
        if (!bloqueada) {
            return true;
        }
        res.setStatus(HttpStatus.LOCKED.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), Map.of("status", 423, "erro", "BARBEARIA_BLOQUEADA",
                "mensagem", "Sua barbearia está bloqueada. Regularize a assinatura para voltar a fazer alterações."));
        return false;
    }
}
