package br.com.mybarber.api.painel;

import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.painel.PainelDtos.*;
import br.com.mybarber.api.repositorio.*;
import br.com.mybarber.api.seguranca.UsuarioLogado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/** Cadastros feitos pela barbearia no portal: dados, barbeiros, horarios, bloqueios e servicos. */
@Service
public class PainelService {

    private final BarbeariaRepository barbearias;
    private final BarbeiroRepository barbeiros;
    private final UsuarioRepository usuarios;
    private final ServicoRepository servicos;
    private final TipoCabeloRepository tiposCabelo;
    private final HorarioTrabalhoRepository horarios;
    private final BloqueioRepository bloqueios;
    private final PasswordEncoder senhas;
    private final Clock relogio;

    public PainelService(BarbeariaRepository barbearias, BarbeiroRepository barbeiros, UsuarioRepository usuarios,
                         ServicoRepository servicos, TipoCabeloRepository tiposCabelo, HorarioTrabalhoRepository horarios,
                         BloqueioRepository bloqueios, PasswordEncoder senhas, Clock relogio) {
        this.barbearias = barbearias;
        this.barbeiros = barbeiros;
        this.usuarios = usuarios;
        this.servicos = servicos;
        this.tiposCabelo = tiposCabelo;
        this.horarios = horarios;
        this.bloqueios = bloqueios;
        this.senhas = senhas;
        this.relogio = relogio;
    }

    // ---------------------------------------------------------------- barbearia

    @Transactional(readOnly = true)
    public BarbeariaDto minhaBarbearia(UsuarioLogado u) {
        Barbearia b = barbearia(u);
        Usuario dono = usuarios.findFirstByBarbeariaIdAndPapelOrderByIdAsc(b.getId(), Papel.ADMIN_BARBEARIA).orElse(null);
        return PainelMapper.barbearia(b, dono, barbeiros.countByBarbeariaIdAndAtivoTrue(b.getId()));
    }

    @Transactional
    public BarbeariaDto atualizarBarbearia(UsuarioLogado u, BarbeariaRequest req) {
        Barbearia b = barbearia(u);
        PainelMapper.aplicar(b, req);
        return minhaBarbearia(u);
    }

    // ---------------------------------------------------------------- barbeiros

    @Transactional(readOnly = true)
    public List<BarbeiroDto> barbeiros(UsuarioLogado u) {
        return barbeiros.findByBarbeariaIdOrderByNomeAsc(u.barbeariaId()).stream().map(PainelMapper::barbeiro).toList();
    }

    @Transactional
    public BarbeiroDto criarBarbeiro(UsuarioLogado u, BarbeiroRequest req) {
        Barbeiro b = new Barbeiro();
        b.setBarbearia(barbearia(u));
        b.setCriadoEm(LocalDateTime.now(relogio));
        aplicarBarbeiro(u, b, req);
        barbeiros.save(b);
        return PainelMapper.barbeiro(b);
    }

    @Transactional
    public BarbeiroDto atualizarBarbeiro(UsuarioLogado u, Long id, BarbeiroRequest req) {
        if (!u.donoBarbearia()) {
            throw ApiException.proibido("Somente o responsável pela barbearia pode editar barbeiros.");
        }
        Barbeiro b = barbeiro(u, id);
        aplicarBarbeiro(u, b, req);
        return PainelMapper.barbeiro(b);
    }

    private void aplicarBarbeiro(UsuarioLogado u, Barbeiro b, BarbeiroRequest req) {
        b.setNome(req.nome().trim());
        b.setTelefone(req.telefone());
        b.setBio(req.bio());
        b.setIntervaloMin(req.intervaloMin());
        if (req.ativo() != null) {
            b.setAtivo(req.ativo());
        }
        b.getTiposCabeloIds().clear();
        if (req.tiposCabeloIds() != null) {
            for (Long t : req.tiposCabeloIds()) {
                if (!tiposCabelo.existsById(t)) throw ApiException.naoEncontrado("Tipo de cabelo");
            }
            b.getTiposCabeloIds().addAll(req.tiposCabeloIds());
        }
        b.getServicosIds().clear();
        if (req.servicosIds() != null) {
            for (Long s : req.servicosIds()) servico(u, s);
            b.getServicosIds().addAll(req.servicosIds());
        }

        // login opcional do barbeiro no portal
        boolean informouEmail = req.email() != null && !req.email().isBlank();
        if (b.getUsuario() == null && informouEmail) {
            if (req.senha() == null) {
                throw ApiException.regra("Informe uma senha inicial para o acesso do barbeiro.");
            }
            String email = req.email().trim().toLowerCase();
            if (usuarios.existsByEmailIgnoreCase(email)) {
                throw ApiException.conflito("Já existe um usuário com este e-mail.");
            }
            Usuario login = new Usuario();
            login.setNome(b.getNome());
            login.setEmail(email);
            login.setSenhaHash(senhas.encode(req.senha()));
            login.setPapel(Papel.BARBEIRO);
            login.setBarbearia(b.getBarbearia());
            login.setTelefone(b.getTelefone());
            login.setCriadoEm(LocalDateTime.now(relogio));
            usuarios.save(login);
            b.setUsuario(login);
        } else if (b.getUsuario() != null) {
            Usuario login = b.getUsuario();
            if (informouEmail && !login.getEmail().equalsIgnoreCase(req.email().trim())) {
                String email = req.email().trim().toLowerCase();
                if (usuarios.existsByEmailIgnoreCase(email)) {
                    throw ApiException.conflito("Já existe um usuário com este e-mail.");
                }
                login.setEmail(email);
            }
            if (req.senha() != null) {
                login.setSenhaHash(senhas.encode(req.senha()));
            }
            if (login.getPapel() == Papel.BARBEIRO) {
                login.setAtivo(b.isAtivo());
                login.setNome(b.getNome());
            }
        }
    }

    // ---------------------------------------------------------------- horarios de trabalho

    @Transactional(readOnly = true)
    public List<JanelaDto> horarios(UsuarioLogado u, Long barbeiroId) {
        barbeiroEditavel(u, barbeiroId);
        return horarios.findByBarbeiroIdOrderByDiaSemanaAscInicioAsc(barbeiroId).stream()
                .map(h -> new JanelaDto(h.getId(), h.getDiaSemana(), h.getInicio(), h.getFim(), new HashSet<>(h.getServicosIds())))
                .toList();
    }

    /** Substitui a semana inteira do barbeiro (expediente, pausas e servicos aceitos por faixa). */
    @Transactional
    public List<JanelaDto> salvarHorarios(UsuarioLogado u, Long barbeiroId, SalvarHorariosRequest req) {
        Barbeiro b = barbeiroEditavel(u, barbeiroId);
        List<JanelaDto> janelas = new ArrayList<>(req.janelas());
        janelas.sort(Comparator.comparingInt(JanelaDto::diaSemana).thenComparing(JanelaDto::inicio));
        for (int i = 0; i < janelas.size(); i++) {
            JanelaDto j = janelas.get(i);
            if (!j.inicio().isBefore(j.fim())) {
                throw ApiException.regra("Em cada faixa, o início deve ser antes do fim.");
            }
            if (i > 0) {
                JanelaDto ant = janelas.get(i - 1);
                if (ant.diaSemana() == j.diaSemana() && j.inicio().isBefore(ant.fim())) {
                    throw ApiException.regra("Existem faixas sobrepostas no mesmo dia.");
                }
            }
            if (j.servicosIds() != null) {
                for (Long s : j.servicosIds()) servico(u, s);
            }
        }
        horarios.deleteAll(horarios.findByBarbeiroIdOrderByDiaSemanaAscInicioAsc(barbeiroId));
        horarios.flush();
        for (JanelaDto j : janelas) {
            HorarioTrabalho h = new HorarioTrabalho();
            h.setBarbeiro(b);
            h.setDiaSemana(j.diaSemana());
            h.setInicio(j.inicio());
            h.setFim(j.fim());
            if (j.servicosIds() != null) h.getServicosIds().addAll(j.servicosIds());
            horarios.save(h);
        }
        return horarios(u, barbeiroId);
    }

    // ---------------------------------------------------------------- bloqueios

    @Transactional(readOnly = true)
    public List<BloqueioDto> bloqueios(UsuarioLogado u, Long barbeiroId) {
        barbeiroEditavel(u, barbeiroId);
        return bloqueios.findByBarbeiroIdAndFimAfterOrderByInicioAsc(barbeiroId, LocalDateTime.now(relogio).minusDays(1))
                .stream().map(x -> new BloqueioDto(x.getId(), x.getInicio(), x.getFim(), x.getMotivo())).toList();
    }

    @Transactional
    public BloqueioDto criarBloqueio(UsuarioLogado u, Long barbeiroId, BloqueioRequest req) {
        Barbeiro b = barbeiroEditavel(u, barbeiroId);
        if (!req.inicio().isBefore(req.fim())) {
            throw ApiException.regra("O início do bloqueio deve ser antes do fim.");
        }
        Bloqueio x = new Bloqueio();
        x.setBarbeiro(b);
        x.setInicio(req.inicio());
        x.setFim(req.fim());
        x.setMotivo(req.motivo());
        bloqueios.save(x);
        return new BloqueioDto(x.getId(), x.getInicio(), x.getFim(), x.getMotivo());
    }

    @Transactional
    public void removerBloqueio(UsuarioLogado u, Long barbeiroId, Long id) {
        barbeiroEditavel(u, barbeiroId);
        Bloqueio x = bloqueios.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Bloqueio"));
        if (!x.getBarbeiro().getId().equals(barbeiroId)) throw ApiException.naoEncontrado("Bloqueio");
        bloqueios.delete(x);
    }

    // ---------------------------------------------------------------- servicos

    @Transactional(readOnly = true)
    public List<ServicoDto> servicos(UsuarioLogado u) {
        return servicos.findByBarbeariaIdOrderByNomeAsc(u.barbeariaId()).stream().map(PainelMapper::servico).toList();
    }

    @Transactional
    public ServicoDto criarServico(UsuarioLogado u, ServicoRequest req) {
        Servico s = new Servico();
        s.setBarbearia(barbearia(u));
        s.setCriadoEm(LocalDateTime.now(relogio));
        aplicarServico(s, req);
        servicos.save(s);
        return PainelMapper.servico(s);
    }

    @Transactional
    public ServicoDto atualizarServico(UsuarioLogado u, Long id, ServicoRequest req) {
        Servico s = servico(u, id);
        aplicarServico(s, req);
        return PainelMapper.servico(s);
    }

    /** Servicos nao sao apagados (ha historico de atendimentos); apenas desativados. */
    @Transactional
    public void desativarServico(UsuarioLogado u, Long id) {
        servico(u, id).setAtivo(false);
    }

    private void aplicarServico(Servico s, ServicoRequest req) {
        s.setNome(req.nome().trim());
        s.setDescricao(req.descricao());
        s.setPreco(req.preco());
        s.setDuracaoMin(req.duracaoMin());
        if (req.ativo() != null) s.setAtivo(req.ativo());
        s.getDuracoes().clear();
        if (req.duracoes() != null) {
            for (DuracaoTipoDto d : req.duracoes()) {
                if (!tiposCabelo.existsById(d.tipoCabeloId())) throw ApiException.naoEncontrado("Tipo de cabelo");
                s.getDuracoes().put(d.tipoCabeloId(), d.duracaoMin());
            }
        }
    }

    // ---------------------------------------------------------------- apoio

    private Barbearia barbearia(UsuarioLogado u) {
        return barbearias.findById(u.barbeariaId()).orElseThrow(() -> ApiException.naoEncontrado("Barbearia"));
    }

    private Barbeiro barbeiro(UsuarioLogado u, Long id) {
        Barbeiro b = barbeiros.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Barbeiro"));
        if (!b.getBarbearia().getId().equals(u.barbeariaId())) throw ApiException.naoEncontrado("Barbeiro");
        return b;
    }

    /** O dono edita qualquer barbeiro; o barbeiro so a propria agenda. */
    private Barbeiro barbeiroEditavel(UsuarioLogado u, Long id) {
        Barbeiro b = barbeiro(u, id);
        if (u.barbeiro() && !b.getId().equals(u.barbeiroId())) {
            throw ApiException.proibido("Você só pode alterar a sua própria agenda.");
        }
        return b;
    }

    private Servico servico(UsuarioLogado u, Long id) {
        Servico s = servicos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Servico"));
        if (!s.getBarbearia().getId().equals(u.barbeariaId())) throw ApiException.naoEncontrado("Servico");
        return s;
    }
}
