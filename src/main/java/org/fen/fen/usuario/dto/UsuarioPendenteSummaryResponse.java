package org.fen.fen.usuario.dto;

import org.fen.fen.usuario.Role;

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
