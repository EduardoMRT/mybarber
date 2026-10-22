package br.com.mybarber.api.financeiro;

import br.com.mybarber.api.agenda.AgendamentoDtos.AgendamentoDto;
import br.com.mybarber.api.dominio.FormaPagamento;
import br.com.mybarber.api.dominio.TipoLancamento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class FinanceiroDtos {

    private FinanceiroDtos() {
    }

    public record LancamentoDto(Long id, TipoLancamento tipo, String categoria, String descricao, BigDecimal valor,
                                LocalDate data, FormaPagamento formaPagamento, Long agendamentoId,
                                Long barbeiroId, String barbeiroNome) {
    }

    public record LancamentoRequest(@NotNull TipoLancamento tipo,
                                    @NotBlank @Size(max = 60) String categoria,
                                    @Size(max = 255) String descricao,
                                    @NotNull @DecimalMin("0.01") BigDecimal valor,
                                    @NotNull LocalDate data,
                                    FormaPagamento formaPagamento,
                                    Long barbeiroId) {
    }

    public record ExtratoDto(LocalDate de, LocalDate ate, BigDecimal receitas, BigDecimal despesas, BigDecimal saldo,
                             List<LancamentoDto> lancamentos) {
    }

    public record PontoDia(LocalDate data, BigDecimal receitas, BigDecimal despesas) {
    }

    public record ItemValor(String nome, long quantidade, BigDecimal valor) {
    }

    public record DashboardDto(
            LocalDate de, LocalDate ate,
            BigDecimal receitas, BigDecimal despesas, BigDecimal lucro,
            long atendimentosConcluidos, long faltas, long cancelados, long agendados,
            BigDecimal ticketMedio, double taxaFaltas, double ocupacao,
            List<PontoDia> porDia,
            List<ItemValor> porServico,
            List<ItemValor> porBarbeiro,
            List<ItemValor> despesasPorCategoria,
            List<ItemValor> formasPagamento,
            List<AgendamentoDto> proximosHoje) {
    }

    public record ClienteResumoDto(Long clienteId, String nome, String telefone, String tipoCabelo,
                                   long atendimentos, long faltas, LocalDateTime ultimoAtendimento,
                                   LocalDateTime proximoAgendamento, BigDecimal totalGasto) {
    }
}
