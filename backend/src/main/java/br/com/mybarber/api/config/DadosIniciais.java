package br.com.mybarber.api.config;

import br.com.mybarber.api.dominio.Papel;
import br.com.mybarber.api.dominio.Usuario;
import br.com.mybarber.api.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** Cria o super admin na primeira execucao, a partir de ADMIN_EMAIL e ADMIN_PASSWORD. */
@Component
public class DadosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final MyBarberProperties props;
    private final Clock relogio;

    public DadosIniciais(UsuarioRepository usuarios, PasswordEncoder senhas, MyBarberProperties props, Clock relogio) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.props = props;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.existsByPapel(Papel.SUPER_ADMIN)) {
            return;
        }
        String email = props.admin().email();
        String senha = props.admin().senha();
        if (email == null || email.isBlank() || senha == null || senha.length() < 8) {
            log.warn("Nenhum super admin cadastrado. Defina ADMIN_EMAIL e ADMIN_PASSWORD (mín. 8 caracteres) e reinicie.");
            return;
        }
        Usuario u = new Usuario();
        u.setNome(props.admin().nome());
        u.setEmail(email.trim().toLowerCase());
        u.setSenhaHash(senhas.encode(senha));
        u.setPapel(Papel.SUPER_ADMIN);
        u.setCriadoEm(LocalDateTime.now(relogio));
        usuarios.save(u);
        log.info("Super admin {} criado.", u.getEmail());
    }
}
