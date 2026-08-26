package org.fen.fen.repository;

import org.fen.fen.domain.Funcionario;
import org.fen.fen.domain.SituacaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {

    boolean existsByCpf(String cpf);

    Optional<Funcionario> findByUsuarioId(UUID usuarioId);

    @Query("""
            select funcionario
            from Funcionario funcionario
            join fetch funcionario.usuario usuario
            where usuario.situacao = :situacao
            order by usuario.createdAt asc
            """)
    List<Funcionario> findAllByUsuarioSituacaoOrderByUsuarioCreatedAtAsc(
            @Param("situacao") SituacaoUsuario situacao
    );

    @Query("""
            select funcionario
            from Funcionario funcionario
            join fetch funcionario.usuario usuario
            where usuario.situacao = org.fen.fen.domain.SituacaoUsuario.ATIVO
              and usuario.role in (
                  org.fen.fen.domain.Role.ADMIN,
                  org.fen.fen.domain.Role.FARMACEUTICO
              )
            order by funcionario.nome
            """)
    List<Funcionario> findSupervisoresAtivos();
}
