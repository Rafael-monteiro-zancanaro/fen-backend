package org.fen.fen.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.fen.fen.usuario.Role;
import org.fen.fen.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");
    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

    @Test
    void issuesTokenWithUserIdRoleAndExactConfiguredExpiration() {
        JwtService jwtService = service(SECRET, NOW);
        Usuario usuario = usuario(Role.ADMIN);

        IssuedToken result = jwtService.issue(usuario, NOW);

        assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-08-21T20:00:00Z"));
        assertThat(jwtService.parse(result.token()))
                .extracting(JwtPrincipal::userId, JwtPrincipal::role)
                .containsExactly(USUARIO_ID, Role.ADMIN);
    }

    @Test
    void defaultsExpirationToEightHours() {
        JwtService jwtService = new JwtService(
                new JwtProperties(SECRET, null),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        IssuedToken result = jwtService.issue(usuario(Role.ADMIN), NOW);

        assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-08-21T20:00:00Z"));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = service(SECRET, NOW).issue(usuario(Role.FARMACEUTICO), NOW).token();
        JwtService jwtService = service("fedcba9876543210fedcba9876543210", NOW);

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredTokenUsingInjectedClock() {
        String token = service(SECRET, NOW).issue(usuario(Role.ESTAGIARIO), NOW).token();
        JwtService jwtService = service(SECRET, Instant.parse("2026-08-21T20:00:01Z"));

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    private JwtService service(String secret, Instant now) {
        return new JwtService(
                new JwtProperties(secret, Duration.ofHours(8)),
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    private Usuario usuario(Role role) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setEmail("usuario@fen.br");
        usuario.setRole(role);
        return usuario;
    }
}
