package br.com.mybarber.api.agenda;

import br.com.mybarber.api.agenda.AgendamentoDtos.HorariosDto;
import br.com.mybarber.api.agenda.CalculadoraHorarios.Intervalo;
import br.com.mybarber.api.agenda.CalculadoraHorarios.Janela;
import br.com.mybarber.api.agenda.CalculadoraHorarios.Parametros;
import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.repositorio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/** Consulta de horarios livres: liga o banco a CalculadoraHorarios. */
@Service
public class AgendaService {

    /** Ate quantos dias a frente o cliente pode agendar. */
    public static final int HORIZONTE_DIAS = 60;

    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;
    private final HorarioTrabalhoRepository horarios;
    private final BloqueioRepository bloqueios;
    private final AgendamentoRepository agendamentos;
    private final Clock relogio;

    public AgendaService(BarbeiroRepository barbeiros, ServicoRepository servicos, HorarioTrabalhoRepository horarios,
                         BloqueioRepository bloqueios, AgendamentoRepository agendamentos, Clock relogio) {
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.horarios = horarios;
        this.bloqueios = bloqueios;
        this.agendamentos = agendamentos;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public HorariosDto disponiveis(Long barbeiroId, Long servicoId, Long tipoCabeloId, LocalDate data) {
        Barbeiro b = barbeiros.findById(barbeiroId).orElseThrow(() -> ApiException.naoEncontrado("Barbeiro"));
        validarBarbeiroAtendendo(b);
        Servico s = servicoDoBarbeiro(b, servicoId);
        validarTipoCabelo(b, tipoCabeloId);
        int duracao = s.duracaoPara(tipoCabeloId);
        LocalDate hoje = LocalDate.now(relogio);
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(HORIZONTE_DIAS))) {
            return new HorariosDto(data, barbeiroId, servicoId, duracao, List.of());
        }
        Parametros p = parametros(b, s, tipoCabeloId, data, null, true);
        return new HorariosDto(data, barbeiroId, servicoId, duracao, CalculadoraHorarios.disponiveis(p));
    }

    /** Monta os dados que a calculadora precisa para um barbeiro/servico/dia. */
    Parametros parametros(Barbeiro b, Servico s, Long tipoCabeloId, LocalDate data, Long ignorarAgendamentoId,
                          boolean aplicarAntecedencia) {
        Barbearia barbearia = b.getBarbearia();
        List<Janela> janelas = horarios.findByBarbeiroIdAndDiaSemanaOrderByInicioAsc(b.getId(), data.getDayOfWeek().getValue())
                .stream()
                .map(h -> new Janela(h.getInicio(), h.getFim(), new HashSet<>(h.getServicosIds())))
                .toList();
        LocalDateTime ini = data.atStartOfDay();
        LocalDateTime fim = data.plusDays(1).atStartOfDay();
        List<Intervalo> ocupados = agendamentos.ocupando(b.getId(), ini.minusHours(4), fim.plusHours(4),
                        StatusAgendamento.OCUPAM, ignorarAgendamentoId == null ? -1L : ignorarAgendamentoId)
                .stream().map(a -> new Intervalo(a.getInicio(), a.getFim())).toList();
        List<Intervalo> bloqs = bloqueios.sobrepondo(b.getId(), ini, fim).stream()
                .map(x -> new Intervalo(x.getInicio(), x.getFim())).toList();
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDateTime naoAntesDe = aplicarAntecedencia ? agora.plusMinutes(barbearia.getAntecedenciaMinimaMin()) : agora;
        return new Parametros(data, s.getId(), s.duracaoPara(tipoCabeloId), barbearia.getGradeMinutos(),
                b.getIntervaloMin(), janelas, ocupados, bloqs, naoAntesDe);
    }

    void validarBarbeiroAtendendo(Barbeiro b) {
        if (!b.isAtivo()) {
            throw ApiException.regra("Este barbeiro não está atendendo no momento.");
        }
        if (b.getBarbearia().bloqueada()) {
            throw ApiException.regra("Esta barbearia está temporariamente indisponível para agendamentos.");
        }
    }

    Servico servicoDoBarbeiro(Barbeiro b, Long servicoId) {
        Servico s = servicos.findById(servicoId).orElseThrow(() -> ApiException.naoEncontrado("Servico"));
        if (!s.getBarbearia().getId().equals(b.getBarbearia().getId()) || !s.isAtivo()) {
            throw ApiException.naoEncontrado("Servico");
        }
        if (!b.realizaServico(s.getId())) {
            throw ApiException.regra("Este barbeiro não realiza o serviço escolhido.");
        }
        return s;
    }

    void validarTipoCabelo(Barbeiro b, Long tipoCabeloId) {
        if (!b.atendeTipo(tipoCabeloId)) {
            throw ApiException.regra("Este barbeiro não atende o seu tipo de cabelo. Escolha outro profissional.");
        }
    }
}
