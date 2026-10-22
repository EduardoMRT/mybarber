package br.com.mybarber.api.auth;

import br.com.mybarber.api.auth.AuthDtos.*;
import br.com.mybarber.api.seguranca.AuthContexto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final AuthContexto contexto;

    public AuthController(AuthService auth, AuthContexto contexto) {
        this.auth = auth;
        this.contexto = contexto;
    }

    @PostMapping("/login")
    public TokensDto login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/refresh")
    public TokensDto refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.renovar(req);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req);
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public TokensDto registrar(@Valid @RequestBody RegistroRequest req, HttpServletRequest http) {
        return auth.registrar(req, http.getRemoteAddr());
    }

    @GetMapping("/me")
    public UsuarioDto me() {
        return auth.me(contexto.atual().id());
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@Valid @RequestBody TrocarSenhaRequest req) {
        auth.trocarSenha(contexto.atual().id(), req);
    }
}
