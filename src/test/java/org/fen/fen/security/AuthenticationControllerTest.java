package org.fen.fen.security;

import org.fen.fen.error.ApiExceptionHandler;
import org.fen.fen.security.dto.AuthUserResponse;
import org.fen.fen.security.dto.LoginRequest;
import org.fen.fen.security.dto.LoginResponse;
import org.fen.fen.usuario.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-21T20:00:00Z");

    private AuthenticationService authenticationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthenticationController(authenticationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void logsInAndReturnsTokenExpirationAndSafeUserSummary() throws Exception {
        when(authenticationService.login(new LoginRequest("admin@fen.br", "admin123")))
                .thenReturn(new LoginResponse(
                        "jwt-token",
                        EXPIRES_AT,
                        adminResponse()
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@fen.br",
                                  "senha": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-21T20:00:00Z"))
                .andExpect(jsonPath("$.user.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.user.email").value("admin@fen.br"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.senha").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void returnsUniformUnauthorizedBodyForWrongPassword() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("internal password detail"));

        assertUniformUnauthorizedLogin();
    }

    @Test
    void returnsUniformUnauthorizedBodyForUnknownUser() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new UsernameNotFoundException("internal account lookup detail"));

        assertUniformUnauthorizedLogin();
    }

    @Test
    void returnsUniformUnauthorizedBodyForPendingUser() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new DisabledException("internal account detail"));

        assertUniformUnauthorizedLogin();
    }

    @Test
    void returnsCurrentUserFromAuthenticatedPrincipal() throws Exception {
        FenUserDetails principal = new FenUserDetails(
                USUARIO_ID,
                "admin@fen.br",
                "$2a$10$not-exposed",
                Role.ADMIN,
                true
        );
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
        when(authenticationService.currentUser(authentication)).thenReturn(adminResponse());

        mockMvc.perform(get("/api/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.email").value("admin@fen.br"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private void assertUniformUnauthorizedLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@fen.br",
                                  "senha": "incorrect-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(jsonPath("$.*").value(org.hamcrest.Matchers.hasSize(5)));
    }

    private AuthUserResponse adminResponse() {
        return new AuthUserResponse(USUARIO_ID, "admin@fen.br", Role.ADMIN);
    }
}
