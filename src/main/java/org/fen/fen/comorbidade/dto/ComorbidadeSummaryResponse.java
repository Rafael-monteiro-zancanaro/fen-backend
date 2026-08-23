package org.fen.fen.comorbidade.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ComorbidadeSummaryResponse(UUID id, String name, long interactionCount, LocalDateTime createdAt) {}
