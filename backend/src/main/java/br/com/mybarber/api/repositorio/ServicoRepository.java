package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByBarbeariaIdOrderByNomeAsc(Long barbeariaId);

    List<Servico> findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(Long barbeariaId);
}
