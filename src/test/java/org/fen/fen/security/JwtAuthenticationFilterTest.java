package org.fen.fen.security;

import org.fen.fen.usuario.Role;
import org.fen.fen.usuario.AdminUsuarioController;
import org.fen.fen.usuario.SituacaoUsuario;
import org.fen.fen.usuario.Usuario;
import org.fen.fen.usuario.UsuarioController;
import org.fen.fen.usuario.UsuarioRepository;
import org.fen.fen.usuario.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.fen.fen.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");
    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000403");
    private static final ObjectMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenPopulatesUuidEmailAndRole() throws Exception {
        Usuario usuario = usuario();
        UsuarioRepository repository = mock(UsuarioRepository.class);
        when(repository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        JwtService jwtService = jwtService(NOW);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                new FenUserDetailsService(repository)
        );
        MockHttpServletRequest request = bearer(jwtService.issue(usuario, NOW).token());

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("farmaceutico@fen.br");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_FARMACEUTICO");
        assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(FenUserDetails.class, details -> {
            assertThat(details.userId()).isEqualTo(USUARIO_ID);
            assertThat(details.role()).isEqualTo(Role.FARMACEUTICO);
        });
    }

    @Test
    void malformedTokenLeavesContextEmptyAndProtectedRequestReturnsJson401() throws Exception {
        assertRejectedBearerToken("not-a-jwt", jwtService(NOW));
    }

    @Test
    void blankBearerTokenLeavesContextEmptyAndProtectedRequestReturnsJson401() throws Exception {
        assertRejectedBearerToken("", jwtService(NOW));
    }

    @Test
    void expiredTokenLeavesContextEmptyAndProtectedRequestReturnsJson401() throws Exception {
        JwtService issuingService = jwtService(NOW);
        String token = issuingService.issue(usuario(), NOW).token();

        assertRejectedBearerToken(token, jwtService(Instant.parse("2026-08-21T20:00:01Z")));
    }

    @Test
    void accessDeniedHandlerReturnsGenericJson403() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(JSON_MAPPER, fixedClock());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("sensitive authorization detail")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
                .contains("\"status\":403", "\"code\":\"FORBIDDEN\"", "\"message\":\"Acesso negado\"")
                .doesNotContain("sensitive authorization detail");
    }

    private void assertRejectedBearerToken(String token, JwtService jwtService) throws Exception {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                new FenUserDetailsService(repository)
        );
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(JSON_MAPPER, fixedClock());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(bearer(token), response, (request, chainResponse) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            entryPoint.commence(
                    (jakarta.servlet.http.HttpServletRequest) request,
                    (jakarta.servlet.http.HttpServletResponse) chainResponse,
                    new InsufficientAuthenticationException("sensitive authentication detail")
            );
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
                .contains("\"status\":401", "\"code\":\"UNAUTHORIZED\"", "\"message\":\"Autenticação necessária\"")
                .doesNotContain("sensitive authentication detail");
    }

    private MockHttpServletRequest bearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private JwtService jwtService(Instant now) {
        return new JwtService(new JwtProperties(SECRET, Duration.ofHours(8)), Clock.fixed(now, ZoneOffset.UTC));
    }

    private Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setEmail("farmaceutico@fen.br");
        usuario.setPasswordHash("$2a$10$hash");
        usuario.setRole(Role.FARMACEUTICO);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        return usuario;
    }

    @Nested
    @WebMvcTest(controllers = {AdminUsuarioController.class, UsuarioController.class})
    @Import({
            SecurityConfig.class,
            JwtService.class,
            FenUserDetailsService.class,
            JwtAuthenticationFilter.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    @TestPropertySource(properties = {
            "fen.security.jwt.secret=0123456789abcdef0123456789abcdef",
            "fen.security.jwt.expiration=8h",
            "fen.security.cors.allowed-origins=http://localhost:4200"
    })
    class SecurityConfigurationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtService jwtService;

        @MockitoBean
        private UsuarioService usuarioService;

        @MockitoBean
        private UsuarioRepository usuarioRepository;

        @Test
        void permitsRegistrationLoginAndSupervisorEndpoints() throws Exception {
            when(usuarioService.findSupervisoresAtivos()).thenReturn(java.util.List.of());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(post("/api/auth/login"))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/public/supervisores"))
                    .andExpect(status().isOk());
        }

        @Test
        void protectsLegacyLoginAndCurrentUserByDefault() throws Exception {
            mockMvc.perform(post("/api/login"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void unauthenticatedAdminRequestReturnsJson401() throws Exception {
            mockMvc.perform(get("/api/admin/usuarios/pendentes"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        void blankBearerTokenOnProtectedRequestReturnsJson401() throws Exception {
            mockMvc.perform(get("/api/admin/usuarios/pendentes")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer "))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @WithMockUser(roles = "FARMACEUTICO")
        void nonAdminRequestReturnsJson403() throws Exception {
            mockMvc.perform(get("/api/admin/usuarios/pendentes"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminRolePassesMethodAuthorization() throws Exception {
            when(usuarioService.findPendentes()).thenReturn(java.util.List.of());

            mockMvc.perform(get("/api/admin/usuarios/pendentes"))
                    .andExpect(status().isOk());
        }

        @Test
        void staleAdminClaimCannotAuthorizeUserWhoseStoredRoleIsPharmacist() throws Exception {
            Usuario tokenUser = usuario();
            tokenUser.setRole(Role.ADMIN);
            Usuario storedUser = usuario();
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(storedUser));

            String token = jwtService.issue(tokenUser, Instant.now()).token();

            mockMvc.perform(get("/api/admin/usuarios/pendentes")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        void allowsCorsPreflightFromConfiguredOrigin() throws Exception {
            mockMvc.perform(options("/api/auth/login")
                            .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                            "http://localhost:4200"
                    ));
        }

        @Test
        void authenticationManagerUsesFenUserDetailsAndBcrypt() {
            Usuario usuario = usuario();
            usuario.setPasswordHash(passwordEncoder.encode("segredo123"));
            when(usuarioRepository.findByEmailIgnoreCase("farmaceutico@fen.br"))
                    .thenReturn(Optional.of(usuario));

            Authentication result = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            "farmaceutico@fen.br",
                            "segredo123"
                    )
            );

            assertThat(result.getName()).isEqualTo("farmaceutico@fen.br");
            assertThat(result.getAuthorities())
                    .extracting("authority")
                    .contains("ROLE_FARMACEUTICO");
        }
    }
}
