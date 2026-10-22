package br.com.mybarber.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Spring Security com JWT (HS256). O token de acesso e curto; a renovacao usa refresh token
 * guardado (com hash) no banco, que pode ser revogado no logout ou na troca de senha.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtro(HttpSecurity http, ObjectMapper json) throws Exception {
        http
                .csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/registrar", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/actuator/health", "/docs/**", "/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        // area exclusiva do dono da barbearia
                        .requestMatchers("/api/painel/dashboard/**", "/api/painel/financeiro/**",
                                "/api/painel/assinatura/**", "/api/painel/clientes/**").hasRole("ADMIN_BARBEARIA")
                        .requestMatchers(HttpMethod.PUT, "/api/painel/barbearia").hasRole("ADMIN_BARBEARIA")
                        .requestMatchers(HttpMethod.POST, "/api/painel/barbeiros", "/api/painel/servicos").hasRole("ADMIN_BARBEARIA")
                        .requestMatchers(HttpMethod.PUT, "/api/painel/servicos/**").hasRole("ADMIN_BARBEARIA")
                        .requestMatchers(HttpMethod.DELETE, "/api/painel/servicos/**").hasRole("ADMIN_BARBEARIA")
                        .requestMatchers("/api/painel/**").hasAnyRole("ADMIN_BARBEARIA", "BARBEIRO")
                        .requestMatchers("/api/app/**").hasRole("CLIENTE")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwt()))
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(HttpStatus.UNAUTHORIZED.value());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            json.writeValue(res.getOutputStream(), Map.of(
                                    "status", 401, "erro", "NAO_AUTENTICADO",
                                    "mensagem", "Sessão expirada ou inválida. Entre novamente."));
                        }))
                .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    json.writeValue(res.getOutputStream(), Map.of(
                            "status", 403, "erro", "SEM_PERMISSAO",
                            "mensagem", "Você não tem permissão para esta ação."));
                }));
        return http.build();
    }

    private JwtAuthenticationConverter conversorJwt() {
        JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
        papeis.setAuthoritiesClaimName("papeis");
        papeis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeis);
        return conversor;
    }

    @Bean
    public SecretKey chaveJwt(MyBarberProperties props) {
        String segredo = props.jwt().segredo();
        if (segredo == null || segredo.length() < 32) {
            throw new IllegalStateException(
                    "Defina JWT_SECRET com pelo menos 32 caracteres (ex.: openssl rand -base64 48).");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chave) {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(MyBarberProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origens = props.cors().origens();
        cfg.setAllowedOriginPatterns(origens == null || origens.isEmpty() ? List.of("http://localhost:5173") : origens);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", cfg);
        return fonte;
    }
}
