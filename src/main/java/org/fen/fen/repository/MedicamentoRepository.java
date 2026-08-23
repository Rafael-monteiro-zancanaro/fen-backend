package org.fen.fen.repository;

import org.fen.fen.domain.Medicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MedicamentoRepository extends JpaRepository<Medicamento, UUID> {

    @Query("""
            select m from Medicamento m
            where :query = ''
               or lower(m.nome) like lower(concat('%', :query, '%'))
               or lower(m.unidadeMedida) like lower(concat('%', :query, '%'))
               or lower(m.viaAdministracao) like lower(concat('%', :query, '%'))
            order by lower(m.nome), m.id
            """)
    Page<Medicamento> buscar(@Param("query") String query, Pageable pageable);

    @Query("""
            select m from Medicamento m
            where lower(m.nome) like lower(concat('%', :query, '%'))
            order by lower(m.nome), m.id
            """)
    List<Medicamento> autocomplete(@Param("query") String query, Pageable pageable);
}
