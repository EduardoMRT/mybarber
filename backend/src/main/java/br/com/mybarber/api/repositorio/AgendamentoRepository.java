package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Agendamento;
import br.com.mybarber.api.dominio.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /** Atendimentos que ocupam a agenda do barbeiro no intervalo (ignorando um id, ex.: o proprio na remarcacao). */
    @Query("""
            select a from Agendamento a
            where a.barbeiro.id = :barbeiroId
              and a.status in :status
              and a.inicio < :fim and a.fim > :inicio
              and a.id <> :ignorarId
            """)
    List<Agendamento> ocupando(@Param("barbeiroId") Long barbeiroId,
                               @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                               @Param("status") Collection<StatusAgendamento> status,
                               @Param("ignorarId") Long ignorarId);

    @Query("""
            select a from Agendamento a
            join fetch a.servico join fetch a.barbeiro
            left join fetch a.cliente left join fetch a.tipoCabelo
            where a.barbearia.id = :barbeariaId
              and a.inicio >= :de and a.inicio < :ate
              and (:barbeiroId is null or a.barbeiro.id = :barbeiroId)
            order by a.inicio
            """)
    List<Agendamento> daBarbearia(@Param("barbeariaId") Long barbeariaId,
                                  @Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate,
                                  @Param("barbeiroId") Long barbeiroId);

    @Query("""
            select a from Agendamento a
            join fetch a.servico join fetch a.barbeiro join fetch a.barbearia
            where a.cliente.id = :clienteId
            order by a.inicio desc
            """)
    List<Agendamento> doCliente(@Param("clienteId") Long clienteId);

    @Query("""
            select a from Agendamento a
            join fetch a.cliente
            where a.barbearia.id = :barbeariaId and a.cliente is not null
            """)
    List<Agendamento> comClienteDaBarbearia(@Param("barbeariaId") Long barbeariaId);

    @Query("select a from Agendamento a where a.cliente.id = :clienteId and a.inicio > :agora and a.status in :status")
    List<Agendamento> futurosDoCliente(@Param("clienteId") Long clienteId, @Param("agora") LocalDateTime agora,
                                       @Param("status") Collection<StatusAgendamento> status);

    long countByInicioBetween(LocalDateTime de, LocalDateTime ate);
}
