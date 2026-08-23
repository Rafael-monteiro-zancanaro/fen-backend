package org.fen.fen.medicamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicamentoRequest(
        @NotBlank @Size(max = 254) String name,
        @NotBlank @Size(max = 254) String measurementUnit,
        @NotBlank @Size(max = 254) String administrationRoute
) {}
