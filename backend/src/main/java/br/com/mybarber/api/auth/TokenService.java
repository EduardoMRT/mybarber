package br.com.mybarber.api.auth;

import br.com.mybarber.api.config.MyBarberProperties;
import br.com.mybarber.api.dominio.RefreshToken;
import br.com.mybarber.api.dominio.Usuario;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.repositorio.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class TokenService {

    public record Emitidos(String accessToken, String refreshToken, LocalDateTime expiraEm) {
    }

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final JwtEncoder encoder;
    private final RefreshTokenRepository refreshTokens;
    private final MyBarberProperties props;
    private final Clock relogio;

    public TokenService(JwtEncoder encoder, RefreshTokenRepository refreshTokens, MyBarberProperties props, Clock relogio) {
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.props = props;
        this.relogio = relogio;
    }

    @Transactional
    public Emitidos emitir(Usuario usuario, Long barbeiroId) {
        Instant agora = relogio.instant();
        Instant expira = agora.plusSeconds(props.jwt().minutosAcesso() * 60L);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("mybarber")
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("papel", usuario.getPapel().name())
                .claim("papeis", List.of(usuario.getPapel().name()))
                .claim("nome", usuario.getNome());
        if (usuario.getBarbearia() != null) {
            claims.claim("barbeariaId", usuario.getBarbearia().getId());
        }
        if (barbeiroId != null) {
            claims.claim("barbeiroId", barbeiroId);
        }
        String access = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();

        byte[] bruto = new byte[32];
        ALEATORIO.nextBytes(bruto);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bruto);

        RefreshToken rt = new RefreshToken();
        rt.setUsuario(usuario);
        rt.setTokenHash(hash(refresh));
        rt.setCriadoEm(LocalDateTime.now(relogio));
        rt.setExpiraEm(LocalDateTime.now(relogio).plusDays(props.jwt().diasRefresh()));
        refreshTokens.save(rt);

        return new Emitidos(access, refresh, LocalDateTime.ofInstant(expira, relogio.getZone()));
    }

    /** Valida e consome o refresh token (rotacao: cada refresh so pode ser usado uma vez). */
    @Transactional
    public Usuario consumir(String refresh) {
        RefreshToken rt = refreshTokens.findByTokenHash(hash(refresh))
                .orElseThrow(TokenService::invalido);
        if (rt.isRevogado() || rt.getExpiraEm().isBefore(LocalDateTime.now(relogio))) {
            throw invalido();
        }
        rt.setRevogado(true);
        return rt.getUsuario();
    }

    @Transactional
    public void revogar(String refresh) {
        refreshTokens.findByTokenHash(hash(refresh)).ifPresent(rt -> rt.setRevogado(true));
    }

    @Transactional
    public void revogarTodos(Long usuarioId) {
        refreshTokens.revogarTodos(usuarioId);
    }

    private static ApiException invalido() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Sessão expirada. Entre novamente.");
    }

    static String hash(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
