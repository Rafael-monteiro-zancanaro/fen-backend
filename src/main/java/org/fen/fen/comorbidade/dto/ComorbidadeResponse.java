package org.fen.fen.comorbidade.dto;

import org.fen.fen.medicamento.dto.MedicamentoSummaryResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ComorbidadeResponse(
        UUID id,
        String name,
        List<UUID> medicationInteractionIds,
        List<MedicamentoSummaryResponse> interactionMedications,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
