package org.fen.fen.repository;

import org.fen.fen.domain.AnexoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnexoAtendimentoRepository extends JpaRepository<AnexoAtendimento, UUID> {

    List<AnexoAtendimento> findByServicoFarmaceuticoIdOrderByCreatedAtDesc(UUID servicoFarmaceuticoId);

    Optional<AnexoAtendimento> findByIdAndServicoFarmaceuticoId(UUID id, UUID servicoFarmaceuticoId);

    @Query("""
            select a.servicoFarmaceutico.id as servicoFarmaceuticoId, count(a) as attachmentCount
            from AnexoAtendimento a
            where a.servicoFarmaceutico.id in :servicoFarmaceuticoIds
            group by a.servicoFarmaceutico.id
            """)
    List<AttachmentCount> countByServicoFarmaceuticoIdIn(
            @Param("servicoFarmaceuticoIds") List<UUID> servicoFarmaceuticoIds
    );

    interface AttachmentCount {
        UUID getServicoFarmaceuticoId();
        long getAttachmentCount();
    }
}
