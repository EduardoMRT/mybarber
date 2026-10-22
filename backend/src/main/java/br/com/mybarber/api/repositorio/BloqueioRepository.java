package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Bloqueio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    List<Bloqueio> findByBarbeiroIdAndFimAfterOrderByInicioAsc(Long barbeiroId, LocalDateTime depoisDe);

    @Query("select b from Bloqueio b where b.barbeiro.id = :barbeiroId and b.inicio < :fim and b.fim > :inicio")
    List<Bloqueio> sobrepondo(@Param("barbeiroId") Long barbeiroId,
                              @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
