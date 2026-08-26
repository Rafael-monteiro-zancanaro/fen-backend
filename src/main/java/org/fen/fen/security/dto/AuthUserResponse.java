package org.fen.fen.security.dto;

import org.fen.fen.domain.Role;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        Role role
) {
}
