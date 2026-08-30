package org.fen.fen.servicofarmaceutico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MedicamentoAtendimentoRequest(
        UUID id, @NotNull UUID medicationId, @NotBlank String batch,
        @NotNull LocalDate expirationDate, @NotBlank String dosage,
        String prescriberName, String prescriberRegistration) {}
