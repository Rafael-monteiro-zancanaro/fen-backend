package org.fen.fen.domain.dto;

import org.fen.fen.domain.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioPendenteSummaryResponse(
        UUID id,
        String nome,
        String email,
        String cpf,
        Role role,
        LocalDateTime solicitadoEm
) {
}
