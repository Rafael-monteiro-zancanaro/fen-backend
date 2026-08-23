package org.fen.fen.repository;

import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findAllBySituacaoOrderByCreatedAtAsc(SituacaoUsuario situacao);
}
