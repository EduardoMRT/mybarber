package br.com.mybarber.api.financeiro;

import br.com.mybarber.api.cobranca.CobrancaDtos.AssinaturaDto;
import br.com.mybarber.api.cobranca.CobrancaService;
import br.com.mybarber.api.financeiro.FinanceiroDtos.*;
import br.com.mybarber.api.seguranca.AuthContexto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Area exclusiva do dono da barbearia: financeiro, dashboard, clientes e assinatura. */
@RestController
@RequestMapping("/api/painel")
public class FinanceiroController {

    private final FinanceiroService financeiro;
    private final CobrancaService cobrancas;
    private final AuthContexto contexto;
    private final Clock relogio;

    public FinanceiroController(FinanceiroService financeiro, CobrancaService cobrancas, AuthContexto contexto, Clock relogio) {
        this.financeiro = financeiro;
        this.cobrancas = cobrancas;
        this.contexto = contexto;
        this.relogio = relogio;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate hoje = LocalDate.now(relogio);
        return financeiro.dashboard(contexto.atual(), de != null ? de : hoje.withDayOfMonth(1),
                ate != null ? ate : hoje.withDayOfMonth(hoje.lengthOfMonth()));
    }

    @GetMapping("/financeiro/lancamentos")
    public ExtratoDto extrato(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return financeiro.extrato(contexto.atual(), de, ate);
    }

    @PostMapping("/financeiro/lancamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoDto criar(@Valid @RequestBody LancamentoRequest req) {
        return financeiro.criar(contexto.atual(), req);
    }

    @PutMapping("/financeiro/lancamentos/{id}")
    public LancamentoDto atualizar(@PathVariable Long id, @Valid @RequestBody LancamentoRequest req) {
        return financeiro.atualizar(contexto.atual(), id, req);
    }

    @DeleteMapping("/financeiro/lancamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        financeiro.excluir(contexto.atual(), id);
    }

    @GetMapping("/clientes")
    public List<ClienteResumoDto> clientes() {
        return financeiro.clientes(contexto.atual());
    }

    @GetMapping("/assinatura")
    public AssinaturaDto assinatura() {
        return cobrancas.assinatura(contexto.barbeariaId());
    }
}
