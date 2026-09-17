package org.fen.fen.servicofarmaceutico.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnexoAtendimentoResponse(
        UUID id,
        String nomeOriginal,
        String contentType,
        long tamanho,
        LocalDateTime createdAt
) {
}
