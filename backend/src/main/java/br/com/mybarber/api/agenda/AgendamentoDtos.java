package br.com.mybarber.api.agenda;

import br.com.mybarber.api.dominio.FormaPagamento;
import br.com.mybarber.api.dominio.OrigemAgendamento;
import br.com.mybarber.api.dominio.StatusAgendamento;
import br.com.mybarber.api.dominio.StatusRemarcacao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class AgendamentoDtos {

    private AgendamentoDtos() {
    }

    public record HorariosDto(LocalDate data, Long barbeiroId, Long servicoId, int duracaoMin, List<LocalTime> horarios) {
    }

    public record CriarRequest(@NotNull Long barbeiroId, @NotNull Long servicoId, @NotNull LocalDateTime inicio,
                               @Size(max = 500) String observacao) {
    }

    public record NovoHorarioRequest(@NotNull LocalDateTime inicio) {
    }

    public record RemarcarRequest(@NotNull LocalDateTime novoInicio, @NotBlank @Size(max = 255) String motivo) {
    }

    public record CancelarRequest(@Size(max = 255) String motivo) {
    }

    public record ConcluirRequest(FormaPagamento formaPagamento, @DecimalMin("0.00") BigDecimal valor) {
    }

    public record ManualRequest(
            @NotNull Long barbeiroId,
            @NotNull Long servicoId,
            Long tipoCabeloId,
            @NotBlank @Size(max = 120) String clienteNome,
            @Size(max = 20) String clienteTelefone,
            @NotNull LocalDateTime inicio,
            @DecimalMin("0.00") BigDecimal valor,
            @Size(max = 500) String observacao,
            boolean concluido,
            FormaPagamento formaPagamento) {
    }

    public record RemarcacaoDto(Long id, LocalDateTime inicioAnterior, LocalDateTime inicioSugerido, String motivo,
                                LocalDateTime prazo, StatusRemarcacao status) {
    }

    public record AgendamentoDto(
            Long id,
            Long barbeariaId, String barbeariaNome, String barbeariaEndereco, String barbeariaTelefone,
            Long barbeiroId, String barbeiroNome,
            Long servicoId, String servicoNome,
            Long clienteId, String clienteNome, String clienteTelefone,
            Long tipoCabeloId, String tipoCabeloNome, String observacoesCliente,
            LocalDateTime inicio, LocalDateTime fim, int duracaoMin,
            BigDecimal valor, StatusAgendamento status, OrigemAgendamento origem,
            String observacao, String motivoCancelamento, FormaPagamento formaPagamento,
            RemarcacaoDto remarcacao,
            boolean podeCancelar) {
    }
}
