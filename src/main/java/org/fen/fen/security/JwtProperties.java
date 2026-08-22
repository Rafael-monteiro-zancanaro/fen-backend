package org.fen.fen.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "fen.security.jwt")
public record JwtProperties(String secret, Duration expiration) {

    public JwtProperties {
        Objects.requireNonNull(secret, "JWT secret is required");
        if (expiration == null) {
            expiration = Duration.ofHours(8);
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 256 bits");
        }
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
    }
}
