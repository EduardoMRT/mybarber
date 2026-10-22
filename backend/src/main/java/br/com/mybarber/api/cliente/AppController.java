package br.com.mybarber.api.cliente;

import br.com.mybarber.api.agenda.AgendamentoDtos.*;
import br.com.mybarber.api.agenda.AgendamentoService;
import br.com.mybarber.api.auth.AuthDtos.UsuarioDto;
import br.com.mybarber.api.cliente.ClienteDtos.*;
import br.com.mybarber.api.notificacao.NotificacaoService;
import br.com.mybarber.api.notificacao.NotificacaoService.ResumoDto;
import br.com.mybarber.api.seguranca.AuthContexto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints do app do cliente (papel CLIENTE). */
@RestController
@RequestMapping("/api/app")
public class AppController {

    private final AgendamentoService agendamentos;
    private final ClienteService clientes;
    private final NotificacaoService notificacoes;
    private final AuthContexto contexto;

    public AppController(AgendamentoService agendamentos, ClienteService clientes, NotificacaoService notificacoes,
                         AuthContexto contexto) {
        this.agendamentos = agendamentos;
        this.clientes = clientes;
        this.notificacoes = notificacoes;
        this.contexto = contexto;
    }

    private Long eu() {
        return contexto.atual().id();
    }

    @PutMapping("/perfil")
    public UsuarioDto perfil(@Valid @RequestBody PerfilRequest req) {
        return clientes.atualizarPerfil(eu(), req);
    }

    @GetMapping("/agendamentos")
    public List<AgendamentoDto> meusAgendamentos() {
        return agendamentos.doCliente(eu());
    }

    @GetMapping("/agendamentos/{id}")
    public AgendamentoDto agendamento(@PathVariable Long id) {
        return agendamentos.detalheDoCliente(eu(), id);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoDto agendar(@Valid @RequestBody CriarRequest req) {
        return agendamentos.criarPeloCliente(eu(), req);
    }

    @PostMapping("/agendamentos/{id}/cancelar")
    public AgendamentoDto cancelar(@PathVariable Long id) {
        return agendamentos.cancelarPeloCliente(eu(), id);
    }

    @PostMapping("/agendamentos/{id}/reagendar")
    public AgendamentoDto reagendar(@PathVariable Long id, @Valid @RequestBody NovoHorarioRequest req) {
        return agendamentos.reagendarPeloCliente(eu(), id, req);
    }

    @PostMapping("/agendamentos/{id}/remarcacao/aceitar")
    public AgendamentoDto aceitarRemarcacao(@PathVariable Long id) {
        return agendamentos.aceitarRemarcacao(eu(), id);
    }

    @PostMapping("/agendamentos/{id}/remarcacao/outro-horario")
    public AgendamentoDto outroHorario(@PathVariable Long id, @Valid @RequestBody NovoHorarioRequest req) {
        return agendamentos.escolherOutroHorario(eu(), id, req);
    }

    @GetMapping("/notificacoes")
    public ResumoDto notificacoes() {
        return notificacoes.listar(eu());
    }

    @PostMapping("/notificacoes/{id}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lida(@PathVariable Long id) {
        notificacoes.marcarLida(id, eu());
    }

    @PostMapping("/notificacoes/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void todasLidas() {
        notificacoes.marcarTodas(eu());
    }

    /** LGPD: exporta todos os dados do titular em JSON. */
    @GetMapping("/meus-dados")
    public MeusDadosDto meusDados() {
        return clientes.meusDados(eu());
    }

    /** LGPD: exclui (anonimiza) a conta. */
    @PostMapping("/excluir-conta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirConta(@Valid @RequestBody ExcluirContaRequest req) {
        clientes.excluirConta(eu(), req);
    }
}
