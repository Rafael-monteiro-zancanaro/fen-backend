package org.fen.fen.repository;

import org.fen.fen.domain.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InteracaoRepository extends JpaRepository<Interacao, UUID> {

    @Query("""
            select i from Interacao i
            join fetch i.medicamento
            join fetch i.comorbidade
            where i.medicamento.id in :medicamentoIds
              and i.comorbidade.id in :comorbidadeIds
            """)
    List<Interacao> buscarPares(
            @Param("medicamentoIds") Set<UUID> medicamentoIds,
            @Param("comorbidadeIds") Set<UUID> comorbidadeIds
    );
}
