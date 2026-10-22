package br.com.mybarber.api.painel;

import br.com.mybarber.api.dominio.StatusBarbearia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class PainelDtos {

    private PainelDtos() {
    }

    public record BarbeariaDto(
            Long id, String nome, String documento, String telefone, String email, String descricao,
            String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf,
            BigDecimal latitude, BigDecimal longitude,
            int gradeMinutos, int antecedenciaMinimaMin, int prazoCancelamentoMin, int prazoRespostaRemarcacaoHoras,
            StatusBarbearia status, String motivoBloqueio,
            BigDecimal valorMensalidade, Integer diaVencimento, int diasTolerancia,
            LocalDateTime criadoEm, String donoNome, String donoEmail, long barbeirosAtivos) {
    }

    public record BarbeariaRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 20) String documento,
            @Size(max = 20) String telefone,
            @Email @Size(max = 160) String email,
            @Size(max = 1000) String descricao,
            @Size(max = 9) String cep,
            @Size(max = 160) String logradouro,
            @Size(max = 20) String numero,
            @Size(max = 80) String complemento,
            @Size(max = 80) String bairro,
            @Size(max = 80) String cidade,
            @Size(min = 2, max = 2) String uf,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @Min(5) @Max(60) Integer gradeMinutos,
            @Min(0) @Max(1440) Integer antecedenciaMinimaMin,
            @Min(0) @Max(4320) Integer prazoCancelamentoMin,
            @Min(1) @Max(72) Integer prazoRespostaRemarcacaoHoras) {
    }

    public record BarbeiroDto(Long id, String nome, String telefone, String bio, int intervaloMin, boolean ativo,
                              boolean temLogin, String email, boolean dono,
                              Set<Long> tiposCabeloIds, Set<Long> servicosIds) {
    }

    public record BarbeiroRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 20) String telefone,
            @Size(max = 500) String bio,
            @Min(0) @Max(120) int intervaloMin,
            Boolean ativo,
            Set<Long> tiposCabeloIds,
            Set<Long> servicosIds,
            @Email @Size(max = 160) String email,
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha) {
    }

    public record JanelaDto(Long id, @Min(1) @Max(7) int diaSemana, @NotNull LocalTime inicio, @NotNull LocalTime fim,
                            Set<Long> servicosIds) {
    }

    public record SalvarHorariosRequest(@NotNull @Valid List<JanelaDto> janelas) {
    }

    public record BloqueioDto(Long id, LocalDateTime inicio, LocalDateTime fim, String motivo) {
    }

    public record BloqueioRequest(@NotNull LocalDateTime inicio, @NotNull LocalDateTime fim, @Size(max = 255) String motivo) {
    }

    public record DuracaoTipoDto(@NotNull Long tipoCabeloId, @Min(5) @Max(600) int duracaoMin) {
    }

    public record ServicoDto(Long id, String nome, String descricao, BigDecimal preco, int duracaoMin, boolean ativo,
                             List<DuracaoTipoDto> duracoes) {
    }

    public record ServicoRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 500) String descricao,
            @NotNull @DecimalMin("0.00") BigDecimal preco,
            @Min(5) @Max(600) int duracaoMin,
            Boolean ativo,
            @Valid List<DuracaoTipoDto> duracoes) {
    }
}
