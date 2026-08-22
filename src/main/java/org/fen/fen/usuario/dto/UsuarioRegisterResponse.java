package org.fen.fen.usuario.dto;

import org.fen.fen.usuario.Role;
import org.fen.fen.usuario.SituacaoUsuario;

import java.util.UUID;

public record UsuarioRegisterResponse(
        UUID usuarioId,
        UUID funcionarioId,
        String email,
        Role role,
        SituacaoUsuario situacao
) {
}
