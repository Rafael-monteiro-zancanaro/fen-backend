package org.fen.fen.comorbidade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ComorbidadeRequest(
        @NotBlank @Size(max = 254) String name,
        @NotNull List<@NotNull UUID> medicationInteractionIds
) {}
