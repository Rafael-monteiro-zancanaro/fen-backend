package org.fen.fen.domain.dto;

import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;

import java.util.UUID;

public record UsuarioRegisterResponse(
        UUID usuarioId,
        UUID funcionarioId,
        String email,
        Role role,
        SituacaoUsuario situacao
) {
}
