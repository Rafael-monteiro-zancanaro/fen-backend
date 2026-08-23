package org.fen.fen.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.Usuario;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken issue(Usuario usuario, Instant now) {
        return issue(usuario.getId(), usuario.getRole(), now);
    }

    IssuedToken issue(FenUserDetails principal, Instant now) {
        return issue(principal.userId(), principal.role(), now);
    }

    private IssuedToken issue(UUID userId, Role role, Instant now) {
        Instant expiresAt = now.plus(properties.expiration());
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        try {
            return new JwtPrincipal(
                    UUID.fromString(claims.getSubject()),
                    Role.valueOf(claims.get(ROLE_CLAIM, String.class))
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new MalformedJwtException("Token JWT inválido", exception);
        }
    }
}

record IssuedToken(String token, Instant expiresAt) {
}

record JwtPrincipal(UUID userId, Role role) {
}
