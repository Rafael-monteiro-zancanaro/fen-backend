package org.fen.fen.repository;

import org.fen.fen.domain.MedicamentoAtendimento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MedicamentoAtendimentoRepository extends JpaRepository<MedicamentoAtendimento, UUID> {

    @EntityGraph(attributePaths = "medicamento")
    List<MedicamentoAtendimento> findByServicoFarmaceuticoIdIn(Collection<UUID> atendimentoIds);
}
