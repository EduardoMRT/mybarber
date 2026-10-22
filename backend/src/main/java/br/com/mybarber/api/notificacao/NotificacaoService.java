package br.com.mybarber.api.notificacao;

import br.com.mybarber.api.dominio.*;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.repositorio.NotificacaoRepository;
import br.com.mybarber.api.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Notificacoes dentro do sistema. O app consulta /api/app/notificacoes e exibe cada nova
 * como notificacao local no celular; o portal mostra um contador para o barbeiro.
 */
@Service
public class NotificacaoService {

    public record NotificacaoDto(Long id, TipoNotificacao tipo, String titulo, String mensagem,
                                 Long agendamentoId, boolean lida, LocalDateTime criadoEm) {
    }

    public record ResumoDto(long naoLidas, List<NotificacaoDto> itens) {
    }

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("EEE, dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    private final NotificacaoRepository repo;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    public NotificacaoService(NotificacaoRepository repo, UsuarioRepository usuarios, Clock relogio) {
        this.repo = repo;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    public static String quando(LocalDateTime dt) {
        return QUANDO.format(dt);
    }

    @Transactional
    public void notificar(Usuario usuario, TipoNotificacao tipo, String titulo, String mensagem, Long agendamentoId) {
        if (usuario == null || !usuario.isAtivo()) {
            return;
        }
        Notificacao n = new Notificacao();
        n.setUsuario(usuario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem.length() > 500 ? mensagem.substring(0, 500) : mensagem);
        n.setAgendamentoId(agendamentoId);
        n.setCriadoEm(LocalDateTime.now(relogio));
        repo.save(n);
    }

    /** Avisa o barbeiro do atendimento; se ele nao tiver login, avisa o dono da barbearia. */
    @Transactional
    public void notificarBarbearia(Agendamento a, TipoNotificacao tipo, String titulo, String mensagem) {
        Usuario destino = a.getBarbeiro().getUsuario();
        if (destino == null) {
            destino = usuarios.findFirstByBarbeariaIdAndPapelOrderByIdAsc(a.getBarbearia().getId(), Papel.ADMIN_BARBEARIA)
                    .orElse(null);
        }
        notificar(destino, tipo, titulo, mensagem, a.getId());
    }

    @Transactional
    public void notificarCliente(Agendamento a, TipoNotificacao tipo, String titulo, String mensagem) {
        notificar(a.getCliente(), tipo, titulo, mensagem, a.getId());
    }

    @Transactional(readOnly = true)
    public ResumoDto listar(Long usuarioId) {
        List<NotificacaoDto> itens = repo.findTop50ByUsuarioIdOrderByCriadoEmDescIdDesc(usuarioId).stream()
                .map(n -> new NotificacaoDto(n.getId(), n.getTipo(), n.getTitulo(), n.getMensagem(),
                        n.getAgendamentoId(), n.isLida(), n.getCriadoEm()))
                .toList();
        return new ResumoDto(repo.countByUsuarioIdAndLidaFalse(usuarioId), itens);
    }

    @Transactional
    public void marcarLida(Long id, Long usuarioId) {
        Notificacao n = repo.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Notificação"));
        if (!n.getUsuario().getId().equals(usuarioId)) {
            throw ApiException.naoEncontrado("Notificação");
        }
        n.setLida(true);
    }

    @Transactional
    public void marcarTodas(Long usuarioId) {
        repo.marcarTodasLidas(usuarioId);
    }
}
