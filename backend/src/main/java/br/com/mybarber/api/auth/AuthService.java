package br.com.mybarber.api.auth;

import br.com.mybarber.api.auth.AuthDtos.*;
import br.com.mybarber.api.config.MyBarberProperties;
import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.repositorio.BarbeiroRepository;
import br.com.mybarber.api.repositorio.ConsentimentoRepository;
import br.com.mybarber.api.repositorio.TipoCabeloRepository;
import br.com.mybarber.api.repositorio.UsuarioRepository;
import br.com.mybarber.api.seguranca.TentativasLogin;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AuthService {

    public static final String DOC_TERMOS = "TERMOS_DE_USO";
    public static final String DOC_PRIVACIDADE = "POLITICA_DE_PRIVACIDADE";

    private final UsuarioRepository usuarios;
    private final BarbeiroRepository barbeiros;
    private final TipoCabeloRepository tiposCabelo;
    private final ConsentimentoRepository consentimentos;
    private final PasswordEncoder senhas;
    private final TokenService tokens;
    private final TentativasLogin tentativas;
    private final MyBarberProperties props;
    private final Clock relogio;

    public AuthService(UsuarioRepository usuarios, BarbeiroRepository barbeiros, TipoCabeloRepository tiposCabelo,
                       ConsentimentoRepository consentimentos, PasswordEncoder senhas, TokenService tokens,
                       TentativasLogin tentativas, MyBarberProperties props, Clock relogio) {
        this.usuarios = usuarios;
        this.barbeiros = barbeiros;
        this.tiposCabelo = tiposCabelo;
        this.consentimentos = consentimentos;
        this.senhas = senhas;
        this.tokens = tokens;
        this.tentativas = tentativas;
        this.props = props;
        this.relogio = relogio;
    }

    @Transactional
    public TokensDto login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        if (tentativas.bloqueado(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MUITAS_TENTATIVAS",
                    "Muitas tentativas. Aguarde alguns minutos e tente de novo.");
        }
        Usuario u = usuarios.findByEmailIgnoreCase(email).orElse(null);
        if (u == null || !u.isAtivo() || u.getExcluidoEm() != null || !senhas.matches(req.senha(), u.getSenhaHash())) {
            tentativas.falhou(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos.");
        }
        tentativas.sucesso(email);
        return emitir(u);
    }

    @Transactional
    public TokensDto renovar(RefreshRequest req) {
        Usuario u = tokens.consumir(req.refreshToken());
        if (!u.isAtivo() || u.getExcluidoEm() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Conta desativada.");
        }
        return emitir(u);
    }

    @Transactional
    public void logout(RefreshRequest req) {
        tokens.revogar(req.refreshToken());
    }

    @Transactional
    public TokensDto registrar(RegistroRequest req, String ip) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflito("Já existe uma conta com este e-mail.");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(senhas.encode(req.senha()));
        u.setTelefone(req.telefone());
        u.setPapel(Papel.CLIENTE);
        u.setSexo(req.sexo());
        u.setComprimentoCabelo(req.comprimentoCabelo());
        if (req.tipoCabeloId() != null) {
            u.setTipoCabelo(tiposCabelo.findById(req.tipoCabeloId())
                    .orElseThrow(() -> ApiException.naoEncontrado("Tipo de cabelo")));
        }
        u.setCriadoEm(LocalDateTime.now(relogio));
        usuarios.save(u);
        registrarConsentimento(u, DOC_TERMOS, ip);
        registrarConsentimento(u, DOC_PRIVACIDADE, ip);
        return emitir(u);
    }

    @Transactional
    public void registrarConsentimento(Usuario u, String documento, String ip) {
        Consentimento c = new Consentimento();
        c.setUsuario(u);
        c.setDocumento(documento);
        c.setVersao(props.legal().versao());
        c.setAceitoEm(LocalDateTime.now(relogio));
        c.setIp(ip);
        consentimentos.save(c);
    }

    @Transactional(readOnly = true)
    public UsuarioDto me(Long usuarioId) {
        return dto(usuarios.findById(usuarioId).orElseThrow(() -> ApiException.naoEncontrado("Usuário")));
    }

    @Transactional
    public void trocarSenha(Long usuarioId, TrocarSenhaRequest req) {
        Usuario u = usuarios.findById(usuarioId).orElseThrow(() -> ApiException.naoEncontrado("Usuário"));
        if (!senhas.matches(req.senhaAtual(), u.getSenhaHash())) {
            throw ApiException.regra("Senha atual incorreta.");
        }
        u.setSenhaHash(senhas.encode(req.novaSenha()));
        tokens.revogarTodos(u.getId());
    }

    private TokensDto emitir(Usuario u) {
        Long barbeiroId = null;
        if (u.getPapel() == Papel.BARBEIRO || u.getPapel() == Papel.ADMIN_BARBEARIA) {
            Barbeiro b = barbeiros.findByUsuarioId(u.getId()).orElse(null);
            if (b != null) {
                if (u.getPapel() == Papel.BARBEIRO && !b.isAtivo()) {
                    throw ApiException.proibido("Seu cadastro de barbeiro está inativo. Fale com a barbearia.");
                }
                barbeiroId = b.getId();
            } else if (u.getPapel() == Papel.BARBEIRO) {
                throw ApiException.proibido("Usuário sem cadastro de barbeiro.");
            }
        }
        TokenService.Emitidos t = tokens.emitir(u, barbeiroId);
        return new TokensDto(t.accessToken(), t.refreshToken(), t.expiraEm(), dto(u, barbeiroId));
    }

    public UsuarioDto dto(Usuario u) {
        Long barbeiroId = barbeiros.findByUsuarioId(u.getId()).map(Barbeiro::getId).orElse(null);
        return dto(u, barbeiroId);
    }

    private UsuarioDto dto(Usuario u, Long barbeiroId) {
        Barbearia b = u.getBarbearia();
        TipoCabelo t = u.getTipoCabelo();
        return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPapel(),
                b != null ? b.getId() : null, b != null ? b.getNome() : null,
                b != null && b.bloqueada(), b != null ? b.getMotivoBloqueio() : null,
                barbeiroId,
                u.getSexo(), t != null ? t.getId() : null, t != null ? t.getNome() : null,
                u.getComprimentoCabelo(), u.getObservacoes());
    }
}
