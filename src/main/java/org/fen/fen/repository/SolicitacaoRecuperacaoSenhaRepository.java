package org.fen.fen.repository;

import org.fen.fen.domain.SolicitacaoRecuperacaoSenha;
import org.fen.fen.domain.StatusSolicitacaoRecuperacaoSenha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRecuperacaoSenhaRepository extends JpaRepository<SolicitacaoRecuperacaoSenha, UUID> {
    @Query("""
            select s from SolicitacaoRecuperacaoSenha s join s.usuario u
            where (:email = '' or lower(u.email) like lower(concat('%', :email, '%')))
              and (:status is null or s.status = :status)
            order by s.createdAt desc, s.id desc
            """)
    Page<SolicitacaoRecuperacaoSenha> buscar(@Param("email") String email,
                                              @Param("status") StatusSolicitacaoRecuperacaoSenha status,
                                              Pageable pageable);

    @EntityGraph(attributePaths = "usuario")
    @Query("select s from SolicitacaoRecuperacaoSenha s where s.id = :id")
    Optional<SolicitacaoRecuperacaoSenha> buscarDetalhe(@Param("id") UUID id);
}
