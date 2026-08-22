package org.fen.fen.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {

    Optional<Funcionario> findByUsuarioId(UUID usuarioId);

    @Query("""
            select funcionario
            from Funcionario funcionario
            join fetch funcionario.usuario usuario
            where usuario.situacao = org.fen.fen.usuario.SituacaoUsuario.ATIVO
              and usuario.role in (
                  org.fen.fen.usuario.Role.ADMIN,
                  org.fen.fen.usuario.Role.FARMACEUTICO
              )
            order by funcionario.nome
            """)
    List<Funcionario> findSupervisoresAtivos();
}
