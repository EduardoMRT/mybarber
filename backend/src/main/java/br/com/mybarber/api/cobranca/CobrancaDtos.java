package br.com.mybarber.api.cobranca;

import br.com.mybarber.api.dominio.Cobranca;
import br.com.mybarber.api.dominio.FormaPagamento;
import br.com.mybarber.api.dominio.StatusBarbearia;
import br.com.mybarber.api.dominio.StatusCobranca;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CobrancaDtos {

    private CobrancaDtos() {
    }

    public record CobrancaDto(Long id, Long barbeariaId, String barbeariaNome, String competencia, BigDecimal valor,
                              LocalDate vencimento, StatusCobranca status, LocalDate pagoEm,
                              FormaPagamento formaPagamento, String observacao) {
        public static CobrancaDto de(Cobranca c) {
            return new CobrancaDto(c.getId(), c.getBarbearia().getId(), c.getBarbearia().getNome(), c.getCompetencia(),
                    c.getValor(), c.getVencimento(), c.getStatus(), c.getPagoEm(), c.getFormaPagamento(), c.getObservacao());
        }
    }

    public record NovaCobrancaRequest(@NotNull Long barbeariaId,
                                      @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "Use o formato AAAA-MM") String competencia,
                                      @NotNull @DecimalMin("0.01") BigDecimal valor,
                                      @NotNull LocalDate vencimento,
                                      @Size(max = 255) String observacao) {
    }

    public record PagarRequest(LocalDate pagoEm, FormaPagamento formaPagamento, @Size(max = 255) String observacao) {
    }

    public record GerarRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "Use o formato AAAA-MM") String competencia) {
    }

    public record GeradasDto(String competencia, int geradas) {
    }

    public record AssinaturaDto(StatusBarbearia status, String motivoBloqueio, BigDecimal valorMensalidade,
                                Integer diaVencimento, int diasTolerancia, List<CobrancaDto> cobrancas) {
    }
}
