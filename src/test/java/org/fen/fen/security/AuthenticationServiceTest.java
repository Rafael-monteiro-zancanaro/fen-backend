package org.fen.fen.security;

import org.fen.fen.security.dto.AuthUserResponse;
import org.fen.fen.security.dto.LoginRequest;
import org.fen.fen.security.dto.LoginResponse;
import org.fen.fen.domain.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-21T20:00:00Z");
    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");

    @Test
    void authenticatesCredentialsThroughAuthenticationManagerAndReturnsIssuedToken() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        FenUserDetails principal = principal();
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(jwtService.issue(eq(principal), eq(NOW)))
                .thenReturn(new IssuedToken("jwt-token", EXPIRES_AT));
        AuthenticationService service = service(authenticationManager, jwtService);

        LoginResponse result = service.login(new LoginRequest("admin@fen.br", "admin123"));

        ArgumentCaptor<Authentication> credentials = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(credentials.capture());
        assertThat(credentials.getValue())
                .extracting(Authentication::getName, Authentication::getCredentials)
                .containsExactly("admin@fen.br", "admin123");
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(result.user()).isEqualTo(new AuthUserResponse(
                USUARIO_ID,
                "admin@fen.br",
                Role.ADMIN
        ));
    }

    @Test
    void returnsCurrentUserFromAuthenticatedJwtPrincipal() {
        AuthenticationService service = service(
                mock(AuthenticationManager.class),
                mock(JwtService.class)
        );
        FenUserDetails principal = principal();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );

        AuthUserResponse result = service.currentUser(authentication);

        assertThat(result).isEqualTo(new AuthUserResponse(
                USUARIO_ID,
                "admin@fen.br",
                Role.ADMIN
        ));
    }

    private AuthenticationService service(
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        return new AuthenticationService(
                authenticationManager,
                jwtService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private FenUserDetails principal() {
        return new FenUserDetails(
                USUARIO_ID,
                "admin@fen.br",
                "$2a$10$not-exposed",
                Role.ADMIN,
                true
        );
    }
}
