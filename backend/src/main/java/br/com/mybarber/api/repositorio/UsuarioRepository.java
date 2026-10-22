package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.Papel;
import br.com.mybarber.api.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPapel(Papel papel);

    long countByPapelAndAtivoTrue(Papel papel);

    Optional<Usuario> findFirstByBarbeariaIdAndPapelOrderByIdAsc(Long barbeariaId, Papel papel);
}
