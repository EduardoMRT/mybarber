package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Barbearia;
import br.com.mybarber.api.dominio.StatusBarbearia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BarbeariaRepository extends JpaRepository<Barbearia, Long> {

    @Query("""
            select b from Barbearia b
            where b.status = :status
              and b.latitude between :latMin and :latMax
              and b.longitude between :lngMin and :lngMax
            """)
    List<Barbearia> buscarNaArea(@Param("status") StatusBarbearia status,
                                 @Param("latMin") BigDecimal latMin, @Param("latMax") BigDecimal latMax,
                                 @Param("lngMin") BigDecimal lngMin, @Param("lngMax") BigDecimal lngMax);

    @Query("""
            select b from Barbearia b
            where b.status = :status
              and (:termo is null or lower(b.nome) like :termo or lower(b.cidade) like :termo or lower(b.bairro) like :termo)
            order by b.nome
            """)
    List<Barbearia> buscarPorTexto(@Param("status") StatusBarbearia status, @Param("termo") String termo);

    List<Barbearia> findAllByOrderByNomeAsc();

    long countByStatus(StatusBarbearia status);
}
