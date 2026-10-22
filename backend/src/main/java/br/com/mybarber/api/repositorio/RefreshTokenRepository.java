package br.com.mybarber.api.repositorio;

import br.com.mybarber.api.dominio.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken r set r.revogado = true where r.usuario.id = :usuarioId and r.revogado = false")
    int revogarTodos(@Param("usuarioId") Long usuarioId);
}
