package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Remarcacao;
import br.com.mybarber.api.dominio.StatusRemarcacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RemarcacaoRepository extends JpaRepository<Remarcacao, Long> {

    Optional<Remarcacao> findFirstByAgendamentoIdAndStatusOrderByCriadoEmDesc(Long agendamentoId, StatusRemarcacao status);

    List<Remarcacao> findByStatusAndPrazoBefore(StatusRemarcacao status, LocalDateTime prazo);

    List<Remarcacao> findByAgendamentoIdInAndStatus(List<Long> agendamentoIds, StatusRemarcacao status);
}
