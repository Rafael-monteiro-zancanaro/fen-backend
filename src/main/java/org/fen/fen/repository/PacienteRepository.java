package org.fen.fen.repository;

import org.fen.fen.domain.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PacienteRepository extends JpaRepository<Paciente, UUID> {
    @EntityGraph(attributePaths = "comorbidades")
    Optional<Paciente> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    @EntityGraph(attributePaths = "comorbidades")
    @Query("select p from Paciente p where p.id = :id") Optional<Paciente> buscarDetalhe(@Param("id") UUID id);
    @Query(value = "select p from Paciente p where :query = '' or lower(p.nome) like lower(concat('%', :query, '%')) or p.cpf like concat('%', :digits, '%') order by lower(p.nome), p.id",
            countQuery = "select count(p) from Paciente p where :query = '' or lower(p.nome) like lower(concat('%', :query, '%')) or p.cpf like concat('%', :digits, '%')")
    Page<Paciente> buscar(@Param("query") String query, @Param("digits") String digits, Pageable pageable);
}
