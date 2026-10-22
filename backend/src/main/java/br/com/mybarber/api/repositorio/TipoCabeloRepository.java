package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.TipoCabelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoCabeloRepository extends JpaRepository<TipoCabelo, Long> {

    List<TipoCabelo> findAllByOrderByOrdemAscNomeAsc();

    List<TipoCabelo> findByAtivoTrueOrderByOrdemAscNomeAsc();
}
