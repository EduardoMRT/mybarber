package br.com.mybarber.api.cliente;

import br.com.mybarber.api.agenda.AgendamentoService;
import br.com.mybarber.api.auth.AuthDtos.UsuarioDto;
import br.com.mybarber.api.auth.AuthService;
import br.com.mybarber.api.auth.TokenService;
import br.com.mybarber.api.cliente.ClienteDtos.*;
import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.notificacao.NotificacaoService;
import br.com.mybarber.api.repositorio.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

@Service
public class ClienteService {

    private final UsuarioRepository usuarios;
    private final TipoCabeloRepository tiposCabelo;
    private final ConsentimentoRepository consentimentos;
    private final AgendamentoRepository agendamentos;
    private final AgendamentoService agendamentoService;
    private final NotificacaoService notificacoes;
    private final AuthService auth;
    private final TokenService tokens;
    private final PasswordEncoder senhas;
    private final Clock relogio;

    public ClienteService(UsuarioRepository usuarios, TipoCabeloRepository tiposCabelo,
                          ConsentimentoRepository consentimentos, AgendamentoRepository agendamentos,
                          AgendamentoService agendamentoService, NotificacaoService notificacoes, AuthService auth,
                          TokenService tokens, PasswordEncoder senhas, Clock relogio) {
        this.usuarios = usuarios;
        this.tiposCabelo = tiposCabelo;
        this.consentimentos = consentimentos;
        this.agendamentos = agendamentos;
        this.agendamentoService = agendamentoService;
        this.notificacoes = notificacoes;
        this.auth = auth;
        this.tokens = tokens;
        this.senhas = senhas;
        this.relogio = relogio;
    }

    @Transactional
    public UsuarioDto atualizarPerfil(Long usuarioId, PerfilRequest req) {
        Usuario u = usuario(usuarioId);
        u.setNome(req.nome().trim());
        u.setTelefone(req.telefone());
        u.setSexo(req.sexo());
        u.setComprimentoCabelo(req.comprimentoCabelo());
        u.setObservacoes(req.observacoes());
        u.setTipoCabelo(req.tipoCabeloId() == null ? null : tiposCabelo.findById(req.tipoCabeloId())
                .orElseThrow(() -> ApiException.naoEncontrado("Tipo de cabelo")));
        return auth.dto(u);
    }

    @Transactional(readOnly = true)
    public MeusDadosDto meusDados(Long usuarioId) {
        Usuario u = usuario(usuarioId);
        return new MeusDadosDto(LocalDateTime.now(relogio), auth.dto(u),
                consentimentos.findByUsuarioIdOrderByAceitoEmDesc(usuarioId).stream()
                        .map(c -> new ConsentimentoDto(c.getDocumento(), c.getVersao(), c.getAceitoEm())).toList(),
                agendamentoService.doCliente(usuarioId),
                notificacoes.listar(usuarioId).itens());
    }

    /**
     * LGPD art. 18, VI: eliminacao. Os dados pessoais sao anonimizados; os atendimentos ja
     * realizados ficam sem identificacao, porque a barbearia precisa deles na contabilidade.
     */
    @Transactional
    public void excluirConta(Long usuarioId, ExcluirContaRequest req) {
        Usuario u = usuario(usuarioId);
        if (!senhas.matches(req.senha(), u.getSenhaHash())) {
            throw ApiException.regra("Senha incorreta.");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        for (Agendamento a : agendamentos.futurosDoCliente(usuarioId, agora,
                EnumSet.of(StatusAgendamento.CONFIRMADO, StatusAgendamento.AGUARDANDO_CLIENTE))) {
            agendamentoService.cancelarPorExclusaoDeConta(a);
        }
        for (Agendamento a : agendamentos.doCliente(usuarioId)) {
            a.setClienteNome("Cliente removido");
            a.setClienteTelefone(null);
        }
        u.setNome("Usuário removido");
        u.setEmail("removido-" + u.getId() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@mybarber.invalid");
        u.setTelefone(null);
        u.setSexo(null);
        u.setTipoCabelo(null);
        u.setComprimentoCabelo(null);
        u.setObservacoes(null);
        u.setSenhaHash(senhas.encode(UUID.randomUUID().toString()));
        u.setAtivo(false);
        u.setExcluidoEm(agora);
        tokens.revogarTodos(usuarioId);
    }

    private Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Usuário"));
    }
}
