package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Consentimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {

    List<Consentimento> findByUsuarioIdOrderByAceitoEmDesc(Long usuarioId);
}
