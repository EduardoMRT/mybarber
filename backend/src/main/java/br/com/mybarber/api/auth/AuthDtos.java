package br.com.mybarber.api.auth;

import br.com.mybarber.api.dominio.ComprimentoCabelo;
import br.com.mybarber.api.dominio.Papel;
import br.com.mybarber.api.dominio.Sexo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record RegistroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
            @Size(max = 20) String telefone,
            Sexo sexo,
            Long tipoCabeloId,
            ComprimentoCabelo comprimentoCabelo,
            @AssertTrue(message = "É necessário aceitar os Termos de Uso") boolean aceiteTermos,
            @AssertTrue(message = "É necessário aceitar a Política de Privacidade") boolean aceitePrivacidade) {
    }

    public record TrocarSenhaRequest(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String novaSenha) {
    }

    public record UsuarioDto(
            Long id, String nome, String email, String telefone, Papel papel,
            Long barbeariaId, String barbeariaNome, boolean barbeariaBloqueada, String motivoBloqueio,
            Long barbeiroId,
            Sexo sexo, Long tipoCabeloId, String tipoCabeloNome, ComprimentoCabelo comprimentoCabelo,
            String observacoes) {
    }

    public record TokensDto(String accessToken, String refreshToken, LocalDateTime expiraEm, UsuarioDto usuario) {
    }
}
