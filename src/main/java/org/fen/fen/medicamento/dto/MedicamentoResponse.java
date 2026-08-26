package org.fen.fen.medicamento.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicamentoResponse(
        UUID id,
        String name,
        String measurementUnit,
        String administrationRoute,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
