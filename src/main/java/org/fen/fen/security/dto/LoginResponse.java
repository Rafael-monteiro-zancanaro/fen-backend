package org.fen.fen.security.dto;

import java.time.Instant;

public record LoginResponse(
        String token,
        Instant expiresAt,
        AuthUserResponse user
) {
}
