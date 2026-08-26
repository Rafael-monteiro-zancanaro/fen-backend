package org.fen.fen.medicamento.dto;

import java.util.UUID;

public record MedicamentoSummaryResponse(
        UUID id,
        String name,
        String measurementUnit,
        String administrationRoute
) {}
