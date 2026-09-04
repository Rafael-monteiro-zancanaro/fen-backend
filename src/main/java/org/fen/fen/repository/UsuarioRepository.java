package org.fen.fen.repository;

import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    @Query(value = "select * from usuario where id = :id for update", nativeQuery = true)
    Optional<Usuario> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findAllBySituacaoOrderByCreatedAtAsc(SituacaoUsuario situacao);
}
