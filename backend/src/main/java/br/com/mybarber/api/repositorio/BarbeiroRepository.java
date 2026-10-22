package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Barbeiro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {

    List<Barbeiro> findByBarbeariaIdOrderByNomeAsc(Long barbeariaId);

    List<Barbeiro> findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(Long barbeariaId);

    Optional<Barbeiro> findByUsuarioId(Long usuarioId);

    long countByBarbeariaIdAndAtivoTrue(Long barbeariaId);

    /** RN06: trava o barbeiro para que dois clientes nao reservem o mesmo horario ao mesmo tempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barbeiro b where b.id = :id")
    Optional<Barbeiro> travar(@Param("id") Long id);
}
