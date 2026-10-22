package br.com.mybarber.api.admin;

import br.com.mybarber.api.admin.AdminDtos.*;
import br.com.mybarber.api.auth.TokenService;
import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.painel.PainelDtos.BarbeariaDto;
import br.com.mybarber.api.painel.PainelMapper;
import br.com.mybarber.api.repositorio.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Gestao da plataforma pelo dono do MyBarber (super admin). */
@Service
public class AdminService {

    private final BarbeariaRepository barbearias;
    private final BarbeiroRepository barbeiros;
    private final UsuarioRepository usuarios;
    private final CobrancaRepository cobrancas;
    private final AgendamentoRepository agendamentos;
    private final TipoCabeloRepository tiposCabelo;
    private final TokenService tokens;
    private final PasswordEncoder senhas;
    private final Clock relogio;

    public AdminService(BarbeariaRepository barbearias, BarbeiroRepository barbeiros, UsuarioRepository usuarios,
                        CobrancaRepository cobrancas, AgendamentoRepository agendamentos,
                        TipoCabeloRepository tiposCabelo, TokenService tokens, PasswordEncoder senhas, Clock relogio) {
        this.barbearias = barbearias;
        this.barbeiros = barbeiros;
        this.usuarios = usuarios;
        this.cobrancas = cobrancas;
        this.agendamentos = agendamentos;
        this.tiposCabelo = tiposCabelo;
        this.tokens = tokens;
        this.senhas = senhas;
        this.relogio = relogio;
    }

    // ---------------------------------------------------------------- barbearias

    @Transactional(readOnly = true)
    public List<BarbeariaDto> barbearias(String busca, StatusBarbearia status) {
        String t = busca == null ? "" : busca.trim().toLowerCase();
        return barbearias.findAllByOrderByNomeAsc().stream()
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> t.isEmpty() || b.getNome().toLowerCase().contains(t)
                        || (b.getCidade() != null && b.getCidade().toLowerCase().contains(t)))
                .map(this::dto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BarbeariaDto barbearia(Long id) {
        return dto(buscar(id));
    }

    @Transactional
    public BarbeariaDto criar(CriarBarbeariaRequest req) {
        String email = req.donoEmail().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflito("Já existe um usuário com o e-mail " + email + ".");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        Barbearia b = new Barbearia();
        PainelMapper.aplicar(b, req.dados());
        aplicarCobranca(b, req.cobranca());
        b.setCriadoEm(agora);
        barbearias.save(b);

        Usuario dono = new Usuario();
        dono.setNome(req.donoNome().trim());
        dono.setEmail(email);
        dono.setSenhaHash(senhas.encode(req.donoSenha()));
        dono.setTelefone(b.getTelefone());
        dono.setPapel(Papel.ADMIN_BARBEARIA);
        dono.setBarbearia(b);
        dono.setCriadoEm(agora);
        usuarios.save(dono);

        if (req.donoAtende()) {
            Barbeiro barbeiro = new Barbeiro();
            barbeiro.setBarbearia(b);
            barbeiro.setUsuario(dono);
            barbeiro.setNome(dono.getNome());
            barbeiro.setTelefone(dono.getTelefone());
            barbeiro.setCriadoEm(agora);
            barbeiros.save(barbeiro);
        }
        return dto(b);
    }

    @Transactional
    public BarbeariaDto atualizar(Long id, AtualizarBarbeariaRequest req) {
        Barbearia b = buscar(id);
        PainelMapper.aplicar(b, req.dados());
        aplicarCobranca(b, req.cobranca());
        return dto(b);
    }

    @Transactional
    public BarbeariaDto bloquear(Long id, BloquearRequest req) {
        Barbearia b = buscar(id);
        b.setStatus(StatusBarbearia.BLOQUEADA);
        b.setBloqueioAutomatico(false);
        b.setMotivoBloqueio(req.motivo().trim());
        return dto(b);
    }

    @Transactional
    public BarbeariaDto desbloquear(Long id) {
        Barbearia b = buscar(id);
        b.setStatus(StatusBarbearia.ATIVA);
        b.setBloqueioAutomatico(false);
        b.setMotivoBloqueio(null);
        return dto(b);
    }

    @Transactional
    public void redefinirSenhaDono(Long id, RedefinirSenhaRequest req) {
        Usuario dono = usuarios.findFirstByBarbeariaIdAndPapelOrderByIdAsc(buscar(id).getId(), Papel.ADMIN_BARBEARIA)
                .orElseThrow(() -> ApiException.naoEncontrado("Responsável da barbearia"));
        dono.setSenhaHash(senhas.encode(req.novaSenha()));
        tokens.revogarTodos(dono.getId());
    }

    private void aplicarCobranca(Barbearia b, ConfigCobranca c) {
        if (c == null) return;
        b.setValorMensalidade(c.valorMensalidade());
        b.setDiaVencimento(c.diaVencimento());
        if (c.diasTolerancia() != null) b.setDiasTolerancia(c.diasTolerancia());
    }

    private Barbearia buscar(Long id) {
        return barbearias.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Barbearia"));
    }

    private BarbeariaDto dto(Barbearia b) {
        Usuario dono = usuarios.findFirstByBarbeariaIdAndPapelOrderByIdAsc(b.getId(), Papel.ADMIN_BARBEARIA).orElse(null);
        return PainelMapper.barbearia(b, dono, barbeiros.countByBarbeariaIdAndAtivoTrue(b.getId()));
    }

    // ---------------------------------------------------------------- dashboard

    @Transactional(readOnly = true)
    public AdminDashboardDto dashboard() {
        LocalDate hoje = LocalDate.now(relogio);
        String competencia = YearMonth.from(hoje).toString();
        List<Barbearia> todas = barbearias.findAll();
        BigDecimal previsto = todas.stream()
                .filter(b -> b.getStatus() == StatusBarbearia.ATIVA && b.getValorMensalidade() != null)
                .map(Barbearia::getValorMensalidade).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Cobranca> lista = cobrancas.todas();
        BigDecimal recebido = lista.stream()
                .filter(c -> c.getStatus() == StatusCobranca.PAGA && c.getPagoEm() != null
                        && YearMonth.from(c.getPagoEm()).equals(YearMonth.from(hoje)))
                .map(Cobranca::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal emAberto = lista.stream()
                .filter(c -> c.getCompetencia().equals(competencia)
                        && (c.getStatus() == StatusCobranca.PENDENTE || c.getStatus() == StatusCobranca.ATRASADA))
                .map(Cobranca::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Cobranca> atrasadas = lista.stream().filter(c -> c.getStatus() == StatusCobranca.ATRASADA).toList();
        BigDecimal valorAtrasado = atrasadas.stream().map(Cobranca::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminDashboardDto(competencia, todas.size(),
                barbearias.countByStatus(StatusBarbearia.ATIVA), barbearias.countByStatus(StatusBarbearia.BLOQUEADA),
                previsto, recebido, emAberto, atrasadas.size(), valorAtrasado,
                usuarios.countByPapelAndAtivoTrue(Papel.CLIENTE),
                agendamentos.countByInicioBetween(hoje.withDayOfMonth(1).atStartOfDay(),
                        hoje.withDayOfMonth(1).plusMonths(1).atStartOfDay()));
    }

    // ---------------------------------------------------------------- tipos de cabelo

    @Transactional(readOnly = true)
    public List<TipoCabeloAdminDto> tiposCabelo() {
        return tiposCabelo.findAllByOrderByOrdemAscNomeAsc().stream().map(AdminService::dto).toList();
    }

    @Transactional
    public TipoCabeloAdminDto criarTipo(TipoCabeloRequest req) {
        TipoCabelo t = new TipoCabelo();
        aplicarTipo(t, req);
        tiposCabelo.save(t);
        return dto(t);
    }

    @Transactional
    public TipoCabeloAdminDto atualizarTipo(Long id, TipoCabeloRequest req) {
        TipoCabelo t = tiposCabelo.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Tipo de cabelo"));
        aplicarTipo(t, req);
        return dto(t);
    }

    private static void aplicarTipo(TipoCabelo t, TipoCabeloRequest req) {
        t.setNome(req.nome().trim());
        t.setDescricao(req.descricao());
        t.setOrdem(req.ordem());
        if (req.ativo() != null) t.setAtivo(req.ativo());
    }

    private static TipoCabeloAdminDto dto(TipoCabelo t) {
        return new TipoCabeloAdminDto(t.getId(), t.getNome(), t.getDescricao(), t.getOrdem(), t.isAtivo());
    }
}
