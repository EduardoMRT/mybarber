package br.com.mybarber.api.painel;

import br.com.mybarber.api.agenda.AgendamentoDtos.*;
import br.com.mybarber.api.agenda.AgendamentoService;
import br.com.mybarber.api.notificacao.NotificacaoService;
import br.com.mybarber.api.notificacao.NotificacaoService.ResumoDto;
import br.com.mybarber.api.painel.PainelDtos.*;
import br.com.mybarber.api.publico.PublicoDtos.TipoCabeloDto;
import br.com.mybarber.api.publico.PublicoService;
import br.com.mybarber.api.seguranca.AuthContexto;
import br.com.mybarber.api.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Portal da barbearia (dono e barbeiros). */
@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService painel;
    private final AgendamentoService agendamentos;
    private final NotificacaoService notificacoes;
    private final PublicoService publico;
    private final AuthContexto contexto;

    public PainelController(PainelService painel, AgendamentoService agendamentos, NotificacaoService notificacoes,
                            PublicoService publico, AuthContexto contexto) {
        this.painel = painel;
        this.agendamentos = agendamentos;
        this.notificacoes = notificacoes;
        this.publico = publico;
        this.contexto = contexto;
    }

    private UsuarioLogado eu() {
        return contexto.atual();
    }

    // barbearia
    @GetMapping("/barbearia")
    public BarbeariaDto barbearia() {
        return painel.minhaBarbearia(eu());
    }

    @PutMapping("/barbearia")
    public BarbeariaDto atualizarBarbearia(@Valid @RequestBody BarbeariaRequest req) {
        return painel.atualizarBarbearia(eu(), req);
    }

    @GetMapping("/tipos-cabelo")
    public List<TipoCabeloDto> tiposCabelo() {
        return publico.tiposCabelo();
    }

    // barbeiros
    @GetMapping("/barbeiros")
    public List<BarbeiroDto> barbeiros() {
        return painel.barbeiros(eu());
    }

    @PostMapping("/barbeiros")
    @ResponseStatus(HttpStatus.CREATED)
    public BarbeiroDto criarBarbeiro(@Valid @RequestBody BarbeiroRequest req) {
        return painel.criarBarbeiro(eu(), req);
    }

    @PutMapping("/barbeiros/{id}")
    public BarbeiroDto atualizarBarbeiro(@PathVariable Long id, @Valid @RequestBody BarbeiroRequest req) {
        return painel.atualizarBarbeiro(eu(), id, req);
    }

    @GetMapping("/barbeiros/{id}/horarios")
    public List<JanelaDto> horarios(@PathVariable Long id) {
        return painel.horarios(eu(), id);
    }

    @PutMapping("/barbeiros/{id}/horarios")
    public List<JanelaDto> salvarHorarios(@PathVariable Long id, @Valid @RequestBody SalvarHorariosRequest req) {
        return painel.salvarHorarios(eu(), id, req);
    }

    @GetMapping("/barbeiros/{id}/bloqueios")
    public List<BloqueioDto> bloqueios(@PathVariable Long id) {
        return painel.bloqueios(eu(), id);
    }

    @PostMapping("/barbeiros/{id}/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioDto criarBloqueio(@PathVariable Long id, @Valid @RequestBody BloqueioRequest req) {
        return painel.criarBloqueio(eu(), id, req);
    }

    @DeleteMapping("/barbeiros/{id}/bloqueios/{bloqueioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerBloqueio(@PathVariable Long id, @PathVariable Long bloqueioId) {
        painel.removerBloqueio(eu(), id, bloqueioId);
    }

    // servicos
    @GetMapping("/servicos")
    public List<ServicoDto> servicos() {
        return painel.servicos(eu());
    }

    @PostMapping("/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoDto criarServico(@Valid @RequestBody ServicoRequest req) {
        return painel.criarServico(eu(), req);
    }

    @PutMapping("/servicos/{id}")
    public ServicoDto atualizarServico(@PathVariable Long id, @Valid @RequestBody ServicoRequest req) {
        return painel.atualizarServico(eu(), id, req);
    }

    @DeleteMapping("/servicos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativarServico(@PathVariable Long id) {
        painel.desativarServico(eu(), id);
    }

    // agenda
    @GetMapping("/agendamentos")
    public List<AgendamentoDto> agendamentos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long barbeiroId) {
        return agendamentos.listarDoPainel(eu(), de, ate, barbeiroId);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoDto lancarManual(@Valid @RequestBody ManualRequest req) {
        return agendamentos.criarManual(eu(), req);
    }

    @PostMapping("/agendamentos/{id}/concluir")
    public AgendamentoDto concluir(@PathVariable Long id, @Valid @RequestBody(required = false) ConcluirRequest req) {
        return agendamentos.concluir(eu(), id, req);
    }

    @PostMapping("/agendamentos/{id}/falta")
    public AgendamentoDto falta(@PathVariable Long id) {
        return agendamentos.marcarFalta(eu(), id);
    }

    @PostMapping("/agendamentos/{id}/cancelar")
    public AgendamentoDto cancelar(@PathVariable Long id, @Valid @RequestBody(required = false) CancelarRequest req) {
        return agendamentos.cancelarPeloBarbeiro(eu(), id, req);
    }

    @PostMapping("/agendamentos/{id}/remarcar")
    public AgendamentoDto remarcar(@PathVariable Long id, @Valid @RequestBody RemarcarRequest req) {
        return agendamentos.remarcar(eu(), id, req);
    }

    // notificacoes do usuario do portal
    @GetMapping("/notificacoes")
    public ResumoDto notificacoes() {
        return notificacoes.listar(eu().id());
    }

    @PostMapping("/notificacoes/{id}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lida(@PathVariable Long id) {
        notificacoes.marcarLida(id, eu().id());
    }

    @PostMapping("/notificacoes/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void todasLidas() {
        notificacoes.marcarTodas(eu().id());
    }
}
