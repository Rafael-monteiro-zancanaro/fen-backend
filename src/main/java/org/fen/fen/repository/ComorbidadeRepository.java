package org.fen.fen.repository;

import org.fen.fen.domain.Comorbidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ComorbidadeRepository extends JpaRepository<Comorbidade, UUID> {

    @Query(value = """
            select c.id as id, c.nome as nome, count(i.id) as interactionCount,
                   c.createdAt as createdAt from Comorbidade c left join c.interacoes i
            where :query = '' or lower(c.nome) like lower(concat('%', :query, '%'))
            group by c.id, c.nome, c.createdAt order by lower(c.nome), c.id
            """, countQuery = """
            select count(c) from Comorbidade c
            where :query = '' or lower(c.nome) like lower(concat('%', :query, '%'))
            """)
    Page<ComorbidadeResumoProjection> buscar(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = {"interacoes", "interacoes.medicamento"})
    @Query("select c from Comorbidade c where c.id = :id")
    Optional<Comorbidade> buscarDetalhe(@Param("id") UUID id);
}
