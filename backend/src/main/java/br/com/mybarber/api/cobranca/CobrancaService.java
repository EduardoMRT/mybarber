package br.com.mybarber.api.cobranca;

import br.com.mybarber.api.cobranca.CobrancaDtos.*;
import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.notificacao.NotificacaoService;
import br.com.mybarber.api.repositorio.BarbeariaRepository;
import br.com.mybarber.api.repositorio.CobrancaRepository;
import br.com.mybarber.api.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;

/**
 * Mensalidade das barbearias (o app e gratuito para o cliente final).
 * Controle manual: o super admin define valor e vencimento e marca os pagamentos;
 * a rotina diaria gera as cobrancas do mes, marca atrasos e bloqueia apos a tolerancia.
 */
@Service
public class CobrancaService {

    private static final Logger log = LoggerFactory.getLogger(CobrancaService.class);

    private final CobrancaRepository cobrancas;
    private final BarbeariaRepository barbearias;
    private final UsuarioRepository usuarios;
    private final NotificacaoService notificacoes;
    private final Clock relogio;

    public CobrancaService(CobrancaRepository cobrancas, BarbeariaRepository barbearias, UsuarioRepository usuarios,
                           NotificacaoService notificacoes, Clock relogio) {
        this.cobrancas = cobrancas;
        this.barbearias = barbearias;
        this.usuarios = usuarios;
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<CobrancaDto> listar(StatusCobranca status, String competencia, Long barbeariaId) {
        return cobrancas.todas().stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> competencia == null || competencia.isBlank() || c.getCompetencia().equals(competencia))
                .filter(c -> barbeariaId == null || c.getBarbearia().getId().equals(barbeariaId))
                .map(CobrancaDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssinaturaDto assinatura(Long barbeariaId) {
        Barbearia b = barbearias.findById(barbeariaId).orElseThrow(() -> ApiException.naoEncontrado("Barbearia"));
        List<CobrancaDto> lista = cobrancas.findByBarbeariaIdOrderByCompetenciaDesc(barbeariaId).stream()
                .map(CobrancaDto::de).toList();
        return new AssinaturaDto(b.getStatus(), b.getMotivoBloqueio(), b.getValorMensalidade(), b.getDiaVencimento(),
                b.getDiasTolerancia(), lista);
    }

    @Transactional
    public CobrancaDto criar(NovaCobrancaRequest req) {
        Barbearia b = barbearias.findById(req.barbeariaId()).orElseThrow(() -> ApiException.naoEncontrado("Barbearia"));
        if (cobrancas.existsByBarbeariaIdAndCompetencia(b.getId(), req.competencia())) {
            throw ApiException.conflito("Já existe cobrança desta barbearia para " + req.competencia() + ".");
        }
        Cobranca c = nova(b, req.competencia(), req.valor(), req.vencimento());
        c.setObservacao(req.observacao());
        if (req.vencimento().isBefore(LocalDate.now(relogio))) {
            c.setStatus(StatusCobranca.ATRASADA);
        }
        cobrancas.save(c);
        return CobrancaDto.de(c);
    }

    /** Gera a cobranca da competencia para todas as barbearias com mensalidade configurada. */
    @Transactional
    public GeradasDto gerar(String competencia) {
        YearMonth ym = YearMonth.parse(competencia);
        int geradas = 0;
        for (Barbearia b : barbearias.findAll()) {
            if (b.getValorMensalidade() == null || b.getValorMensalidade().signum() <= 0 || b.getDiaVencimento() == null) {
                continue;
            }
            if (cobrancas.existsByBarbeariaIdAndCompetencia(b.getId(), competencia)) {
                continue;
            }
            int dia = Math.min(b.getDiaVencimento(), ym.lengthOfMonth());
            cobrancas.save(nova(b, competencia, b.getValorMensalidade(), ym.atDay(dia)));
            geradas++;
        }
        return new GeradasDto(competencia, geradas);
    }

    @Transactional
    public CobrancaDto pagar(Long id, PagarRequest req) {
        Cobranca c = cobrancas.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Cobrança"));
        if (c.getStatus() == StatusCobranca.CANCELADA) {
            throw ApiException.regra("Cobrança cancelada não pode ser paga.");
        }
        c.setStatus(StatusCobranca.PAGA);
        c.setPagoEm(req != null && req.pagoEm() != null ? req.pagoEm() : LocalDate.now(relogio));
        if (req != null) {
            c.setFormaPagamento(req.formaPagamento());
            if (req.observacao() != null) c.setObservacao(req.observacao());
        }
        desbloquearSeQuitada(c.getBarbearia());
        return CobrancaDto.de(c);
    }

    @Transactional
    public CobrancaDto reabrir(Long id) {
        Cobranca c = cobrancas.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Cobrança"));
        c.setPagoEm(null);
        c.setFormaPagamento(null);
        c.setStatus(c.getVencimento().isBefore(LocalDate.now(relogio)) ? StatusCobranca.ATRASADA : StatusCobranca.PENDENTE);
        return CobrancaDto.de(c);
    }

    @Transactional
    public CobrancaDto cancelar(Long id) {
        Cobranca c = cobrancas.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Cobrança"));
        c.setStatus(StatusCobranca.CANCELADA);
        desbloquearSeQuitada(c.getBarbearia());
        return CobrancaDto.de(c);
    }

    /** Rotina diaria (00:10): gera o mes corrente, marca atrasos e aplica bloqueio automatico. */
    @Scheduled(cron = "0 10 0 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void rotinaDiaria() {
        LocalDate hoje = LocalDate.now(relogio);
        gerar(YearMonth.from(hoje).toString());
        for (Cobranca c : cobrancas.porStatus(EnumSet.of(StatusCobranca.PENDENTE, StatusCobranca.ATRASADA))) {
            if (c.getStatus() == StatusCobranca.PENDENTE && c.getVencimento().isBefore(hoje)) {
                c.setStatus(StatusCobranca.ATRASADA);
            }
            Barbearia b = c.getBarbearia();
            if (c.getStatus() == StatusCobranca.ATRASADA && !b.bloqueada()
                    && c.getVencimento().plusDays(b.getDiasTolerancia()).isBefore(hoje)) {
                b.setStatus(StatusBarbearia.BLOQUEADA);
                b.setBloqueioAutomatico(true);
                b.setMotivoBloqueio("Mensalidade de " + c.getCompetencia() + " em atraso");
                avisarDono(b, "Barbearia bloqueada",
                        "A mensalidade de " + c.getCompetencia() + " está em atraso. Regularize para voltar a receber agendamentos.");
                log.info("Barbearia {} bloqueada por inadimplência", b.getId());
            }
        }
    }

    private void desbloquearSeQuitada(Barbearia b) {
        if (!b.bloqueada() || !b.isBloqueioAutomatico()) {
            return;
        }
        LocalDate hoje = LocalDate.now(relogio);
        boolean aindaDeve = cobrancas.findByBarbeariaIdAndStatusIn(b.getId(), EnumSet.of(StatusCobranca.ATRASADA)).stream()
                .anyMatch(x -> x.getVencimento().plusDays(b.getDiasTolerancia()).isBefore(hoje));
        if (!aindaDeve) {
            b.setStatus(StatusBarbearia.ATIVA);
            b.setBloqueioAutomatico(false);
            b.setMotivoBloqueio(null);
            avisarDono(b, "Barbearia liberada", "Pagamento registrado. Sua barbearia voltou a aparecer no app.");
        }
    }

    private void avisarDono(Barbearia b, String titulo, String mensagem) {
        usuarios.findFirstByBarbeariaIdAndPapelOrderByIdAsc(b.getId(), Papel.ADMIN_BARBEARIA)
                .ifPresent(u -> notificacoes.notificar(u, TipoNotificacao.ASSINATURA, titulo, mensagem, null));
    }

    private Cobranca nova(Barbearia b, String competencia, BigDecimal valor, LocalDate vencimento) {
        Cobranca c = new Cobranca();
        c.setBarbearia(b);
        c.setCompetencia(competencia);
        c.setValor(valor);
        c.setVencimento(vencimento);
        c.setStatus(StatusCobranca.PENDENTE);
        c.setCriadoEm(LocalDateTime.now(relogio));
        return c;
    }
}
