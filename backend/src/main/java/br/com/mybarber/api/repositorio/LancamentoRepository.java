package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    @Query("""
            select l from Lancamento l left join fetch l.barbeiro
            where l.barbearia.id = :barbeariaId and l.data between :de and :ate
            order by l.data desc, l.id desc
            """)
    List<Lancamento> doPeriodo(@Param("barbeariaId") Long barbeariaId,
                               @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    Optional<Lancamento> findByAgendamentoId(Long agendamentoId);
}
