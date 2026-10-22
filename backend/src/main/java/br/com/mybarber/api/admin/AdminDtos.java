package br.com.mybarber.api.admin;

import br.com.mybarber.api.painel.PainelDtos.BarbeariaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record ConfigCobranca(@DecimalMin("0.00") BigDecimal valorMensalidade,
                                 @Min(1) @Max(31) Integer diaVencimento,
                                 @Min(0) @Max(60) Integer diasTolerancia) {
    }

    public record CriarBarbeariaRequest(@NotNull @Valid BarbeariaRequest dados,
                                        @Valid ConfigCobranca cobranca,
                                        @NotBlank @Size(max = 120) String donoNome,
                                        @NotBlank @Email @Size(max = 160) String donoEmail,
                                        @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String donoSenha,
                                        boolean donoAtende) {
    }

    public record AtualizarBarbeariaRequest(@NotNull @Valid BarbeariaRequest dados, @Valid ConfigCobranca cobranca) {
    }

    public record BloquearRequest(@NotBlank @Size(max = 255) String motivo) {
    }

    public record RedefinirSenhaRequest(@NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String novaSenha) {
    }

    public record TipoCabeloAdminDto(Long id, String nome, String descricao, int ordem, boolean ativo) {
    }

    public record TipoCabeloRequest(@NotBlank @Size(max = 60) String nome, @Size(max = 255) String descricao,
                                    int ordem, Boolean ativo) {
    }

    public record AdminDashboardDto(String competencia, long totalBarbearias, long ativas, long bloqueadas,
                                    BigDecimal receitaMensalPrevista, BigDecimal recebidoNoMes, BigDecimal emAbertoNoMes,
                                    long cobrancasAtrasadas, BigDecimal valorAtrasado,
                                    long totalClientes, long agendamentosNoMes) {
    }
}
