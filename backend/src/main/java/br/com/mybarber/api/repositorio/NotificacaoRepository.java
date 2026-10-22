package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findTop50ByUsuarioIdOrderByCriadoEmDescIdDesc(Long usuarioId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.usuario.id = :usuarioId and n.lida = false")
    int marcarTodasLidas(@Param("usuarioId") Long usuarioId);
}
