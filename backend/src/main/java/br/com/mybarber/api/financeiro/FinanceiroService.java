package br.com.mybarber.api.financeiro;

import br.com.mybarber.api.agenda.AgendamentoDtos.AgendamentoDto;
import br.com.mybarber.api.agenda.AgendamentoService;
import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.financeiro.FinanceiroDtos.*;
import br.com.mybarber.api.repositorio.*;
import br.com.mybarber.api.seguranca.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Parte contabil do portal: lancamentos, extrato, dashboard e carteira de clientes. */
@Service
public class FinanceiroService {

    private final LancamentoRepository lancamentos;
    private final AgendamentoRepository agendamentos;
    private final BarbeariaRepository barbearias;
    private final BarbeiroRepository barbeiros;
    private final HorarioTrabalhoRepository horarios;
    private final AgendamentoService agendamentoService;
    private final Clock relogio;

    public FinanceiroService(LancamentoRepository lancamentos, AgendamentoRepository agendamentos,
                             BarbeariaRepository barbearias, BarbeiroRepository barbeiros,
                             HorarioTrabalhoRepository horarios, AgendamentoService agendamentoService, Clock relogio) {
        this.lancamentos = lancamentos;
        this.agendamentos = agendamentos;
        this.barbearias = barbearias;
        this.barbeiros = barbeiros;
        this.horarios = horarios;
        this.agendamentoService = agendamentoService;
        this.relogio = relogio;
    }

    // ---------------------------------------------------------------- lancamentos

    @Transactional(readOnly = true)
    public ExtratoDto extrato(UsuarioLogado u, LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate);
        List<Lancamento> lista = lancamentos.doPeriodo(u.barbeariaId(), de, ate);
        BigDecimal receitas = soma(lista, TipoLancamento.RECEITA);
        BigDecimal despesas = soma(lista, TipoLancamento.DESPESA);
        return new ExtratoDto(de, ate, receitas, despesas, receitas.subtract(despesas),
                lista.stream().map(FinanceiroService::dto).toList());
    }

    @Transactional
    public LancamentoDto criar(UsuarioLogado u, LancamentoRequest req) {
        Lancamento l = new Lancamento();
        l.setBarbearia(barbearias.getReferenceById(u.barbeariaId()));
        l.setCriadoEm(LocalDateTime.now(relogio));
        aplicar(u, l, req);
        lancamentos.save(l);
        return dto(l);
    }

    @Transactional
    public LancamentoDto atualizar(UsuarioLogado u, Long id, LancamentoRequest req) {
        Lancamento l = doUsuario(u, id);
        aplicar(u, l, req);
        return dto(l);
    }

    @Transactional
    public void excluir(UsuarioLogado u, Long id) {
        lancamentos.delete(doUsuario(u, id));
    }

    private void aplicar(UsuarioLogado u, Lancamento l, LancamentoRequest req) {
        l.setTipo(req.tipo());
        l.setCategoria(req.categoria().trim());
        l.setDescricao(req.descricao());
        l.setValor(req.valor());
        l.setData(req.data());
        l.setFormaPagamento(req.formaPagamento());
        if (req.barbeiroId() != null) {
            Barbeiro b = barbeiros.findById(req.barbeiroId()).orElseThrow(() -> ApiException.naoEncontrado("Barbeiro"));
            if (!b.getBarbearia().getId().equals(u.barbeariaId())) throw ApiException.naoEncontrado("Barbeiro");
            l.setBarbeiro(b);
        } else {
            l.setBarbeiro(null);
        }
    }

    private Lancamento doUsuario(UsuarioLogado u, Long id) {
        Lancamento l = lancamentos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Lançamento"));
        if (!l.getBarbearia().getId().equals(u.barbeariaId())) throw ApiException.naoEncontrado("Lançamento");
        return l;
    }

    // ---------------------------------------------------------------- dashboard

    @Transactional(readOnly = true)
    public DashboardDto dashboard(UsuarioLogado u, LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate);
        Long barbeariaId = u.barbeariaId();
        List<Lancamento> lanc = lancamentos.doPeriodo(barbeariaId, de, ate);
        List<Agendamento> ags = agendamentos.daBarbearia(barbeariaId, de.atStartOfDay(), ate.plusDays(1).atStartOfDay(), null);

        BigDecimal receitas = soma(lanc, TipoLancamento.RECEITA);
        BigDecimal despesas = soma(lanc, TipoLancamento.DESPESA);

        List<Agendamento> concluidos = filtrar(ags, StatusAgendamento.CONCLUIDO);
        long faltas = filtrar(ags, StatusAgendamento.FALTA).size();
        long cancelados = filtrar(ags, StatusAgendamento.CANCELADO).size();
        long agendados = ags.stream().filter(a -> a.getStatus().ativo()).count();
        BigDecimal faturadoServicos = concluidos.stream().map(Agendamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticket = concluidos.isEmpty() ? BigDecimal.ZERO
                : faturadoServicos.divide(BigDecimal.valueOf(concluidos.size()), 2, RoundingMode.HALF_UP);
        double taxaFaltas = (concluidos.size() + faltas) == 0 ? 0 : (double) faltas / (concluidos.size() + faltas);

        long minutosOcupados = ags.stream()
                .filter(a -> StatusAgendamento.OCUPAM.contains(a.getStatus()))
                .mapToLong(a -> Duration.between(a.getInicio(), a.getFim()).toMinutes()).sum();
        long minutosDisponiveis = minutosDeTrabalho(barbeariaId, de, ate);
        double ocupacao = minutosDisponiveis == 0 ? 0 : Math.min(1.0, (double) minutosOcupados / minutosDisponiveis);

        // serie diaria
        Map<LocalDate, BigDecimal[]> dias = new TreeMap<>();
        for (LocalDate d = de; !d.isAfter(ate); d = d.plusDays(1)) {
            dias.put(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Lancamento l : lanc) {
            BigDecimal[] v = dias.get(l.getData());
            if (v == null) continue;
            int i = l.getTipo() == TipoLancamento.RECEITA ? 0 : 1;
            v[i] = v[i].add(l.getValor());
        }
        List<PontoDia> porDia = dias.entrySet().stream()
                .map(e -> new PontoDia(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();

        List<ItemValor> porServico = agrupar(concluidos, a -> a.getServico().getNome(), Agendamento::getValor);
        List<ItemValor> porBarbeiro = agrupar(concluidos, a -> a.getBarbeiro().getNome(), Agendamento::getValor);
        List<ItemValor> despesasCat = agrupar(lanc.stream().filter(l -> l.getTipo() == TipoLancamento.DESPESA).toList(),
                Lancamento::getCategoria, Lancamento::getValor);
        List<ItemValor> formas = agrupar(lanc.stream().filter(l -> l.getTipo() == TipoLancamento.RECEITA).toList(),
                l -> l.getFormaPagamento() != null ? l.getFormaPagamento().name() : "NAO_INFORMADO", Lancamento::getValor);

        LocalDateTime agora = LocalDateTime.now(relogio);
        List<AgendamentoDto> proximos = agendamentos.daBarbearia(barbeariaId, agora, agora.toLocalDate().plusDays(1).atStartOfDay(), null)
                .stream().filter(a -> a.getStatus().ativo()).limit(10)
                .map(a -> agendamentoService.listarUm(a)).toList();

        return new DashboardDto(de, ate, receitas, despesas, receitas.subtract(despesas),
                concluidos.size(), faltas, cancelados, agendados, ticket, taxaFaltas, ocupacao,
                porDia, porServico, porBarbeiro, despesasCat, formas, proximos);
    }

    /** Minutos de expediente dos barbeiros ativos no periodo (base da taxa de ocupacao). */
    private long minutosDeTrabalho(Long barbeariaId, LocalDate de, LocalDate ate) {
        Set<Long> ativos = barbeiros.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId).stream()
                .map(Barbeiro::getId).collect(Collectors.toSet());
        Map<Integer, Long> porDiaSemana = new HashMap<>();
        for (HorarioTrabalho h : horarios.findByBarbeiroBarbeariaId(barbeariaId)) {
            if (!ativos.contains(h.getBarbeiro().getId())) continue;
            long min = Duration.between(h.getInicio(), h.getFim()).toMinutes();
            porDiaSemana.merge(h.getDiaSemana(), min, Long::sum);
        }
        long total = 0;
        for (LocalDate d = de; !d.isAfter(ate); d = d.plusDays(1)) {
            total += porDiaSemana.getOrDefault(d.getDayOfWeek().getValue(), 0L);
        }
        return total;
    }

    // ---------------------------------------------------------------- clientes

    @Transactional(readOnly = true)
    public List<ClienteResumoDto> clientes(UsuarioLogado u) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        Map<Long, List<Agendamento>> porCliente = agendamentos.comClienteDaBarbearia(u.barbeariaId()).stream()
                .collect(Collectors.groupingBy(a -> a.getCliente().getId()));
        List<ClienteResumoDto> lista = new ArrayList<>();
        for (List<Agendamento> ags : porCliente.values()) {
            Usuario c = ags.get(0).getCliente();
            List<Agendamento> concl = filtrar(ags, StatusAgendamento.CONCLUIDO);
            LocalDateTime ultimo = concl.stream().map(Agendamento::getInicio).max(Comparator.naturalOrder()).orElse(null);
            LocalDateTime proximo = ags.stream().filter(a -> a.getStatus().ativo() && a.getInicio().isAfter(agora))
                    .map(Agendamento::getInicio).min(Comparator.naturalOrder()).orElse(null);
            BigDecimal total = concl.stream().map(Agendamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
            lista.add(new ClienteResumoDto(c.getId(), c.getNome(), c.getTelefone(),
                    c.getTipoCabelo() != null ? c.getTipoCabelo().getNome() : null,
                    concl.size(), filtrar(ags, StatusAgendamento.FALTA).size(), ultimo, proximo, total));
        }
        lista.sort(Comparator.comparing(ClienteResumoDto::nome, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    // ---------------------------------------------------------------- apoio

    private void validarPeriodo(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de) || ate.isAfter(de.plusDays(366))) {
            throw ApiException.regra("Período inválido (máximo de 1 ano).");
        }
    }

    private static List<Agendamento> filtrar(List<Agendamento> lista, StatusAgendamento status) {
        return lista.stream().filter(a -> a.getStatus() == status).toList();
    }

    private static BigDecimal soma(List<Lancamento> lista, TipoLancamento tipo) {
        return lista.stream().filter(l -> l.getTipo() == tipo).map(Lancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static <T> List<ItemValor> agrupar(List<T> itens, Function<T, String> chave, Function<T, BigDecimal> valor) {
        Map<String, long[]> qtd = new HashMap<>();
        Map<String, BigDecimal> tot = new HashMap<>();
        for (T t : itens) {
            String k = chave.apply(t);
            qtd.computeIfAbsent(k, x -> new long[1])[0]++;
            tot.merge(k, valor.apply(t), BigDecimal::add);
        }
        return tot.entrySet().stream()
                .map(e -> new ItemValor(e.getKey(), qtd.get(e.getKey())[0], e.getValue()))
                .sorted(Comparator.comparing(ItemValor::valor).reversed())
                .toList();
    }

    private static LancamentoDto dto(Lancamento l) {
        Barbeiro b = l.getBarbeiro();
        return new LancamentoDto(l.getId(), l.getTipo(), l.getCategoria(), l.getDescricao(), l.getValor(), l.getData(),
                l.getFormaPagamento(), l.getAgendamentoId(), b != null ? b.getId() : null, b != null ? b.getNome() : null);
    }
}
