package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.HorarioTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    List<HorarioTrabalho> findByBarbeiroIdOrderByDiaSemanaAscInicioAsc(Long barbeiroId);

    List<HorarioTrabalho> findByBarbeiroIdAndDiaSemanaOrderByInicioAsc(Long barbeiroId, int diaSemana);

    List<HorarioTrabalho> findByBarbeiroBarbeariaId(Long barbeariaId);
}
