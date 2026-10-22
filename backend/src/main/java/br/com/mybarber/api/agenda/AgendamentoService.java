package br.com.mybarber.api.agenda;

import br.com.mybarber.api.agenda.AgendamentoDtos.*;
import br.com.mybarber.api.agenda.CalculadoraHorarios.Parametros;
import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.notificacao.NotificacaoService;
import br.com.mybarber.api.repositorio.*;
import br.com.mybarber.api.seguranca.UsuarioLogado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static br.com.mybarber.api.notificacao.NotificacaoService.quando;

/** Todas as acoes sobre agendamentos: do cliente (app) e da barbearia (portal). */
@Service
public class AgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);

    private final AgendaService agenda;
    private final AgendamentoRepository agendamentos;
    private final RemarcacaoRepository remarcacoes;
    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;
    private final UsuarioRepository usuarios;
    private final TipoCabeloRepository tiposCabelo;
    private final LancamentoRepository lancamentos;
    private final NotificacaoService notificacoes;
    private final Clock relogio;

    public AgendamentoService(AgendaService agenda, AgendamentoRepository agendamentos, RemarcacaoRepository remarcacoes,
                              BarbeiroRepository barbeiros, ServicoRepository servicos, UsuarioRepository usuarios,
                              TipoCabeloRepository tiposCabelo, LancamentoRepository lancamentos,
                              NotificacaoService notificacoes, Clock relogio) {
        this.agenda = agenda;
        this.agendamentos = agendamentos;
        this.remarcacoes = remarcacoes;
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.usuarios = usuarios;
        this.tiposCabelo = tiposCabelo;
        this.lancamentos = lancamentos;
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    // ------------------------------------------------------------------ cliente (app)

    @Transactional
    public AgendamentoDto criarPeloCliente(Long clienteId, CriarRequest req) {
        Usuario cliente = usuarios.findById(clienteId).orElseThrow(() -> ApiException.naoEncontrado("Usuário"));
        Long tipoId = cliente.getTipoCabelo() != null ? cliente.getTipoCabelo().getId() : null;

        Barbeiro b = barbeiros.travar(req.barbeiroId()).orElseThrow(() -> ApiException.naoEncontrado("Barbeiro"));
        agenda.validarBarbeiroAtendendo(b);
        Servico s = agenda.servicoDoBarbeiro(b, req.servicoId());
        agenda.validarTipoCabelo(b, tipoId);
        validarHorizonte(req.inicio());

        Parametros p = agenda.parametros(b, s, tipoId, req.inicio().toLocalDate(), null, true);
        if (!CalculadoraHorarios.ofertado(p, req.inicio().toLocalTime())) {
            throw ApiException.conflito("Esse horário não está mais disponível. Escolha outro.");
        }

        Agendamento a = new Agendamento();
        a.setBarbearia(b.getBarbearia());
        a.setBarbeiro(b);
        a.setServico(s);
        a.setCliente(cliente);
        a.setTipoCabelo(cliente.getTipoCabelo());
        a.setInicio(req.inicio());
        a.setFim(req.inicio().plusMinutes(p.duracaoMin()));
        a.setValor(s.getPreco());
        a.setStatus(StatusAgendamento.CONFIRMADO);
        a.setOrigem(OrigemAgendamento.APP);
        a.setObservacao(req.observacao());
        a.setCriadoEm(agora());
        a.setAtualizadoEm(agora());
        agendamentos.save(a);

        notificacoes.notificarCliente(a, TipoNotificacao.AGENDAMENTO_CONFIRMADO, "Agendamento confirmado",
                s.getNome() + " com " + b.getNome() + " em " + quando(a.getInicio()) + ".");
        notificacoes.notificarBarbearia(a, TipoNotificacao.NOVO_AGENDAMENTO, "Novo agendamento",
                cliente.getNome() + " marcou " + s.getNome() + " para " + quando(a.getInicio()) + ".");
        return dto(a, null);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoDto> doCliente(Long clienteId) {
        List<Agendamento> lista = agendamentos.doCliente(clienteId);
        return comRemarcacoes(lista);
    }

    @Transactional(readOnly = true)
    public AgendamentoDto detalheDoCliente(Long clienteId, Long id) {
        Agendamento a = doClienteOuErro(clienteId, id);
        return dto(a, remarcacaoPendente(a));
    }

    @Transactional
    public AgendamentoDto cancelarPeloCliente(Long clienteId, Long id) {
        Agendamento a = doClienteOuErro(clienteId, id);
        exigirAtivo(a);
        if (a.getStatus() == StatusAgendamento.AGUARDANDO_CLIENTE) {
            fecharRemarcacao(a, StatusRemarcacao.RECUSADA);
        } else {
            exigirPrazoCancelamento(a);
        }
        a.setStatus(StatusAgendamento.CANCELADO);
        a.setMotivoCancelamento("Cancelado pelo cliente");
        a.setAtualizadoEm(agora());
        notificacoes.notificarBarbearia(a, TipoNotificacao.AGENDAMENTO_CANCELADO, "Agendamento cancelado",
                a.nomeCliente() + " cancelou " + a.getServico().getNome() + " de " + quando(a.getInicio()) + ".");
        return dto(a, null);
    }

    /** Cliente excluiu a conta: cancela reservas futuras sem checar prazo. */
    @Transactional
    public void cancelarPorExclusaoDeConta(Agendamento a) {
        fecharRemarcacao(a, StatusRemarcacao.RECUSADA);
        a.setStatus(StatusAgendamento.CANCELADO);
        a.setMotivoCancelamento("Cliente excluiu a conta");
        a.setAtualizadoEm(agora());
        notificacoes.notificarBarbearia(a, TipoNotificacao.AGENDAMENTO_CANCELADO, "Agendamento cancelado",
                "O horário de " + quando(a.getInicio()) + " foi liberado (cliente excluiu a conta).");
    }

    /** RF12: o cliente muda o horario por conta propria (respeitando o prazo minimo). */
    @Transactional
    public AgendamentoDto reagendarPeloCliente(Long clienteId, Long id, NovoHorarioRequest req) {
        Agendamento a = doClienteOuErro(clienteId, id);
        if (a.getStatus() == StatusAgendamento.AGUARDANDO_CLIENTE) {
            return escolherOutroHorario(clienteId, id, req);
        }
        if (a.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw ApiException.regra("Este agendamento não pode mais ser alterado.");
        }
        exigirPrazoCancelamento(a);
        LocalDateTime anterior = a.getInicio();
        moverParaHorarioOfertado(a, req.inicio());
        notificacoes.notificarBarbearia(a, TipoNotificacao.AGENDAMENTO_ALTERADO, "Cliente reagendou",
                a.nomeCliente() + " mudou de " + quando(anterior) + " para " + quando(a.getInicio()) + ".");
        return dto(a, null);
    }

    @Transactional
    public AgendamentoDto aceitarRemarcacao(Long clienteId, Long id) {
        Agendamento a = doClienteOuErro(clienteId, id);
        if (a.getStatus() != StatusAgendamento.AGUARDANDO_CLIENTE) {
            throw ApiException.regra("Não há remarcação aguardando sua resposta.");
        }
        fecharRemarcacao(a, StatusRemarcacao.ACEITA);
        a.setStatus(StatusAgendamento.CONFIRMADO);
        a.setAtualizadoEm(agora());
        notificacoes.notificarBarbearia(a, TipoNotificacao.REMARCACAO_RESPONDIDA, "Remarcação aceita",
                a.nomeCliente() + " aceitou o novo horário: " + quando(a.getInicio()) + ".");
        return dto(a, null);
    }

    @Transactional
    public AgendamentoDto escolherOutroHorario(Long clienteId, Long id, NovoHorarioRequest req) {
        Agendamento a = doClienteOuErro(clienteId, id);
        if (a.getStatus() != StatusAgendamento.AGUARDANDO_CLIENTE) {
            throw ApiException.regra("Não há remarcação aguardando sua resposta.");
        }
        moverParaHorarioOfertado(a, req.inicio());
        fecharRemarcacao(a, StatusRemarcacao.OUTRO_HORARIO);
        a.setStatus(StatusAgendamento.CONFIRMADO);
        notificacoes.notificarBarbearia(a, TipoNotificacao.REMARCACAO_RESPONDIDA, "Cliente escolheu outro horário",
                a.nomeCliente() + " escolheu " + quando(a.getInicio()) + " para " + a.getServico().getNome() + ".");
        return dto(a, null);
    }

    private void moverParaHorarioOfertado(Agendamento a, LocalDateTime novoInicio) {
        validarHorizonte(novoInicio);
        Barbeiro b = barbeiros.travar(a.getBarbeiro().getId()).orElseThrow();
        agenda.validarBarbeiroAtendendo(b);
        Long tipoId = a.getTipoCabelo() != null ? a.getTipoCabelo().getId() : null;
        Parametros p = agenda.parametros(b, a.getServico(), tipoId, novoInicio.toLocalDate(), a.getId(), true);
        if (!CalculadoraHorarios.ofertado(p, novoInicio.toLocalTime())) {
            throw ApiException.conflito("Esse horário não está disponível. Escolha outro.");
        }
        a.setInicio(novoInicio);
        a.setFim(novoInicio.plusMinutes(p.duracaoMin()));
        a.setAtualizadoEm(agora());
    }

    // ------------------------------------------------------------------ barbearia (portal)

    @Transactional(readOnly = true)
    public List<AgendamentoDto> listarDoPainel(UsuarioLogado u, LocalDate de, LocalDate ate, Long barbeiroId) {
        if (ate.isBefore(de) || ate.isAfter(de.plusDays(92))) {
            throw ApiException.regra("Período inválido (máximo de 92 dias).");
        }
        Long filtro = u.barbeiro() ? u.barbeiroId() : barbeiroId;
        List<Agendamento> lista = agendamentos.daBarbearia(u.barbeariaId(), de.atStartOfDay(),
                ate.plusDays(1).atStartOfDay(), filtro);
        return comRemarcacoes(lista);
    }

    /** Corte lancado manualmente pelo barbeiro (cliente de balcao, telefone, etc.). */
    @Transactional
    public AgendamentoDto criarManual(UsuarioLogado u, ManualRequest req) {
        Barbeiro b = barbeiros.travar(req.barbeiroId()).orElseThrow(() -> ApiException.naoEncontrado("Barbeiro"));
        exigirBarbeiroDoUsuario(u, b);
        Servico s = servicos.findById(req.servicoId()).orElseThrow(() -> ApiException.naoEncontrado("Servico"));
        if (!s.getBarbearia().getId().equals(u.barbeariaId())) {
            throw ApiException.naoEncontrado("Servico");
        }
        TipoCabelo tipo = req.tipoCabeloId() == null ? null : tiposCabelo.findById(req.tipoCabeloId())
                .orElseThrow(() -> ApiException.naoEncontrado("Tipo de cabelo"));
        int duracao = s.duracaoPara(req.tipoCabeloId());

        if (!req.concluido()) {
            Parametros p = agenda.parametros(b, s, req.tipoCabeloId(), req.inicio().toLocalDate(), null, false);
            if (!CalculadoraHorarios.semConflito(p, req.inicio())) {
                throw ApiException.conflito("Já existe atendimento ou bloqueio nesse horário.");
            }
        }

        Agendamento a = new Agendamento();
        a.setBarbearia(b.getBarbearia());
        a.setBarbeiro(b);
        a.setServico(s);
        a.setClienteNome(req.clienteNome().trim());
        a.setClienteTelefone(req.clienteTelefone());
        a.setTipoCabelo(tipo);
        a.setInicio(req.inicio());
        a.setFim(req.inicio().plusMinutes(duracao));
        a.setValor(req.valor() != null ? req.valor() : s.getPreco());
        a.setOrigem(OrigemAgendamento.MANUAL);
        a.setObservacao(req.observacao());
        a.setStatus(req.concluido() ? StatusAgendamento.CONCLUIDO : StatusAgendamento.CONFIRMADO);
        a.setFormaPagamento(req.formaPagamento());
        a.setCriadoEm(agora());
        a.setAtualizadoEm(agora());
        agendamentos.save(a);
        if (req.concluido()) {
            lancarReceita(a);
        }
        return dto(a, null);
    }

    @Transactional
    public AgendamentoDto concluir(UsuarioLogado u, Long id, ConcluirRequest req) {
        Agendamento a = doPainelOuErro(u, id);
        if (a.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw ApiException.regra("Só é possível concluir um atendimento confirmado.");
        }
        a.setStatus(StatusAgendamento.CONCLUIDO);
        if (req != null && req.valor() != null) {
            a.setValor(req.valor());
        }
        if (req != null && req.formaPagamento() != null) {
            a.setFormaPagamento(req.formaPagamento());
        }
        a.setAtualizadoEm(agora());
        lancarReceita(a);
        return dto(a, null);
    }

    @Transactional
    public AgendamentoDto marcarFalta(UsuarioLogado u, Long id) {
        Agendamento a = doPainelOuErro(u, id);
        if (a.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw ApiException.regra("Só é possível marcar falta em um atendimento confirmado.");
        }
        if (a.getInicio().isAfter(agora())) {
            throw ApiException.regra("A falta só pode ser marcada depois do horário do atendimento.");
        }
        a.setStatus(StatusAgendamento.FALTA);
        a.setAtualizadoEm(agora());
        return dto(a, null);
    }

    @Transactional
    public AgendamentoDto cancelarPeloBarbeiro(UsuarioLogado u, Long id, CancelarRequest req) {
        Agendamento a = doPainelOuErro(u, id);
        exigirAtivo(a);
        fecharRemarcacao(a, StatusRemarcacao.SUBSTITUIDA);
        String motivo = req != null && req.motivo() != null && !req.motivo().isBlank() ? req.motivo().trim() : null;
        a.setStatus(StatusAgendamento.CANCELADO);
        a.setMotivoCancelamento(motivo != null ? motivo : "Cancelado pela barbearia");
        a.setAtualizadoEm(agora());
        notificacoes.notificarCliente(a, TipoNotificacao.AGENDAMENTO_CANCELADO, "Agendamento cancelado",
                a.getBarbearia().getNome() + " cancelou seu horário de " + quando(a.getInicio())
                        + (motivo != null ? ". Motivo: " + motivo : ".") + " Você pode agendar outro horário no app.");
        return dto(a, null);
    }

    /**
     * RF13/RF14: o barbeiro sugere um novo horario. Se o cliente veio pelo app, a reserva fica
     * "aguardando cliente" ate ele aceitar, escolher outro horario ou cancelar.
     */
    @Transactional
    public AgendamentoDto remarcar(UsuarioLogado u, Long id, RemarcarRequest req) {
        Agendamento a = doPainelOuErro(u, id);
        exigirAtivo(a);
        if (!req.novoInicio().isAfter(agora())) {
            throw ApiException.regra("O novo horário precisa ser no futuro.");
        }
        Barbeiro b = barbeiros.travar(a.getBarbeiro().getId()).orElseThrow();
        Long tipoId = a.getTipoCabelo() != null ? a.getTipoCabelo().getId() : null;
        Parametros p = agenda.parametros(b, a.getServico(), tipoId, req.novoInicio().toLocalDate(), a.getId(), false);
        if (!CalculadoraHorarios.semConflito(p, req.novoInicio())) {
            throw ApiException.conflito("O novo horário conflita com outro atendimento ou bloqueio.");
        }

        LocalDateTime anterior = a.getInicio();
        a.setInicio(req.novoInicio());
        a.setFim(req.novoInicio().plusMinutes(p.duracaoMin()));
        a.setAtualizadoEm(agora());

        if (a.getCliente() == null) {
            a.setStatus(StatusAgendamento.CONFIRMADO);
            return dto(a, null);
        }

        fecharRemarcacao(a, StatusRemarcacao.SUBSTITUIDA);
        Remarcacao r = new Remarcacao();
        r.setAgendamento(a);
        r.setInicioAnterior(anterior);
        r.setInicioSugerido(req.novoInicio());
        r.setMotivo(req.motivo().trim());
        r.setStatus(StatusRemarcacao.PENDENTE);
        LocalDateTime prazo = agora().plusHours(a.getBarbearia().getPrazoRespostaRemarcacaoHoras());
        r.setPrazo(prazo.isBefore(req.novoInicio()) ? prazo : req.novoInicio());
        r.setCriadoEm(agora());
        remarcacoes.save(r);

        a.setStatus(StatusAgendamento.AGUARDANDO_CLIENTE);
        notificacoes.notificarCliente(a, TipoNotificacao.REMARCACAO_PROPOSTA, "Sua reserva mudou",
                a.getBarbearia().getNome() + " sugeriu " + quando(r.getInicioSugerido()) + " no lugar de "
                        + quando(anterior) + ". Motivo: " + r.getMotivo()
                        + ". Abra o app para aceitar, escolher outro horário ou cancelar.");
        return dto(a, r);
    }

    /** Remarcacoes sem resposta dentro do prazo cancelam a reserva automaticamente. */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    @Transactional
    public void expirarRemarcacoes() {
        List<Remarcacao> vencidas = remarcacoes.findByStatusAndPrazoBefore(StatusRemarcacao.PENDENTE, agora());
        for (Remarcacao r : vencidas) {
            r.setStatus(StatusRemarcacao.EXPIRADA);
            r.setRespondidoEm(agora());
            Agendamento a = r.getAgendamento();
            if (a.getStatus() == StatusAgendamento.AGUARDANDO_CLIENTE) {
                a.setStatus(StatusAgendamento.CANCELADO);
                a.setMotivoCancelamento("Remarcação sem resposta do cliente");
                a.setAtualizadoEm(agora());
                notificacoes.notificarCliente(a, TipoNotificacao.REMARCACAO_EXPIRADA, "Reserva cancelada",
                        "Sua reserva de " + quando(a.getInicio()) + " foi cancelada porque a remarcação não foi respondida a tempo.");
                notificacoes.notificarBarbearia(a, TipoNotificacao.REMARCACAO_EXPIRADA, "Remarcação expirada",
                        a.nomeCliente() + " não respondeu a remarcação; o horário de " + quando(a.getInicio()) + " foi liberado.");
            }
        }
        if (!vencidas.isEmpty()) {
            log.info("{} remarcação(ões) expirada(s)", vencidas.size());
        }
    }

    // ------------------------------------------------------------------ apoio

    private void lancarReceita(Agendamento a) {
        if (lancamentos.findByAgendamentoId(a.getId()).isPresent()) {
            return;
        }
        Lancamento l = new Lancamento();
        l.setBarbearia(a.getBarbearia());
        l.setTipo(TipoLancamento.RECEITA);
        l.setCategoria("Serviços");
        l.setDescricao(a.getServico().getNome() + " - " + a.nomeCliente());
        l.setValor(a.getValor());
        l.setData(a.getInicio().toLocalDate());
        l.setFormaPagamento(a.getFormaPagamento());
        l.setAgendamentoId(a.getId());
        l.setBarbeiro(a.getBarbeiro());
        l.setCriadoEm(agora());
        lancamentos.save(l);
    }

    private Agendamento doClienteOuErro(Long clienteId, Long id) {
        Agendamento a = agendamentos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Agendamento"));
        if (a.getCliente() == null || !a.getCliente().getId().equals(clienteId)) {
            throw ApiException.naoEncontrado("Agendamento");
        }
        return a;
    }

    private Agendamento doPainelOuErro(UsuarioLogado u, Long id) {
        Agendamento a = agendamentos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Agendamento"));
        if (!a.getBarbearia().getId().equals(u.barbeariaId())) {
            throw ApiException.naoEncontrado("Agendamento");
        }
        if (u.barbeiro() && !a.getBarbeiro().getId().equals(u.barbeiroId())) {
            throw ApiException.proibido("Este atendimento é de outro barbeiro.");
        }
        return a;
    }

    private void exigirBarbeiroDoUsuario(UsuarioLogado u, Barbeiro b) {
        if (!b.getBarbearia().getId().equals(u.barbeariaId())) {
            throw ApiException.naoEncontrado("Barbeiro");
        }
        if (u.barbeiro() && !b.getId().equals(u.barbeiroId())) {
            throw ApiException.proibido("Você só pode lançar atendimentos na sua própria agenda.");
        }
    }

    private void exigirAtivo(Agendamento a) {
        if (!a.getStatus().ativo()) {
            throw ApiException.regra("Este agendamento já foi encerrado.");
        }
    }

    private void exigirPrazoCancelamento(Agendamento a) {
        int prazo = a.getBarbearia().getPrazoCancelamentoMin();
        if (agora().plusMinutes(prazo).isAfter(a.getInicio())) {
            throw ApiException.regra("Alterações e cancelamentos só podem ser feitos até " + descreverMinutos(prazo)
                    + " antes do horário. Fale com a barbearia.");
        }
    }

    private void validarHorizonte(LocalDateTime inicio) {
        if (inicio.toLocalDate().isAfter(LocalDate.now(relogio).plusDays(AgendaService.HORIZONTE_DIAS))) {
            throw ApiException.regra("Só é possível agendar com até " + AgendaService.HORIZONTE_DIAS + " dias de antecedência.");
        }
    }

    private void fecharRemarcacao(Agendamento a, StatusRemarcacao status) {
        remarcacoes.findFirstByAgendamentoIdAndStatusOrderByCriadoEmDesc(a.getId(), StatusRemarcacao.PENDENTE)
                .ifPresent(r -> {
                    r.setStatus(status);
                    r.setRespondidoEm(agora());
                });
    }

    private Remarcacao remarcacaoPendente(Agendamento a) {
        if (a.getStatus() != StatusAgendamento.AGUARDANDO_CLIENTE) {
            return null;
        }
        return remarcacoes.findFirstByAgendamentoIdAndStatusOrderByCriadoEmDesc(a.getId(), StatusRemarcacao.PENDENTE)
                .orElse(null);
    }

    private List<AgendamentoDto> comRemarcacoes(List<Agendamento> lista) {
        List<Long> aguardando = lista.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGUARDANDO_CLIENTE)
                .map(Agendamento::getId).toList();
        Map<Long, Remarcacao> porAgendamento = aguardando.isEmpty() ? Map.of()
                : remarcacoes.findByAgendamentoIdInAndStatus(aguardando, StatusRemarcacao.PENDENTE).stream()
                .collect(Collectors.toMap(r -> r.getAgendamento().getId(), Function.identity(), (x, y) -> y));
        return lista.stream().map(a -> dto(a, porAgendamento.get(a.getId()))).toList();
    }

    private static String descreverMinutos(int minutos) {
        if (minutos % 60 == 0) {
            int h = minutos / 60;
            return h + (h == 1 ? " hora" : " horas");
        }
        return minutos + " minutos";
    }

    /** Converte um agendamento ja carregado (usado pelo dashboard). */
    public AgendamentoDto listarUm(Agendamento a) {
        return dto(a, remarcacaoPendente(a));
    }

    AgendamentoDto dto(Agendamento a, Remarcacao r) {
        Barbearia b = a.getBarbearia();
        TipoCabelo t = a.getTipoCabelo();
        Usuario c = a.getCliente();
        boolean podeCancelar = a.getStatus() == StatusAgendamento.AGUARDANDO_CLIENTE
                || (a.getStatus() == StatusAgendamento.CONFIRMADO
                && !agora().plusMinutes(b.getPrazoCancelamentoMin()).isAfter(a.getInicio()));
        RemarcacaoDto rd = r == null ? null : new RemarcacaoDto(r.getId(), r.getInicioAnterior(), r.getInicioSugerido(),
                r.getMotivo(), r.getPrazo(), r.getStatus());
        return new AgendamentoDto(
                a.getId(),
                b.getId(), b.getNome(), b.enderecoFormatado(), b.getTelefone(),
                a.getBarbeiro().getId(), a.getBarbeiro().getNome(),
                a.getServico().getId(), a.getServico().getNome(),
                c != null ? c.getId() : null, a.nomeCliente(), a.telefoneCliente(),
                t != null ? t.getId() : null, t != null ? t.getNome() : null,
                c != null ? c.getObservacoes() : null,
                a.getInicio(), a.getFim(), (int) Duration.between(a.getInicio(), a.getFim()).toMinutes(),
                a.getValor(), a.getStatus(), a.getOrigem(),
                a.getObservacao(), a.getMotivoCancelamento(), a.getFormaPagamento(),
                rd, podeCancelar);
    }
}
