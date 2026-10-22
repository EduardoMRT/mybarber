package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Cobranca;
import br.com.mybarber.api.dominio.StatusCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    List<Cobranca> findByBarbeariaIdOrderByCompetenciaDesc(Long barbeariaId);

    boolean existsByBarbeariaIdAndCompetencia(Long barbeariaId, String competencia);

    @Query("select c from Cobranca c join fetch c.barbearia order by c.vencimento desc, c.id desc")
    List<Cobranca> todas();

    @Query("select c from Cobranca c join fetch c.barbearia where c.status in :status")
    List<Cobranca> porStatus(@Param("status") Collection<StatusCobranca> status);

    List<Cobranca> findByBarbeariaIdAndStatusIn(Long barbeariaId, Collection<StatusCobranca> status);
}
