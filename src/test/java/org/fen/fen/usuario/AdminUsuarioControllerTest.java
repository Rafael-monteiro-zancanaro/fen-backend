package org.fen.fen.usuario;

import org.fen.fen.error.ApiExceptionHandler;
import org.fen.fen.usuario.dto.SupervisorResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteDetailResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUsuarioControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID FUNCIONARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID SUPERVISOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000303");

    private UsuarioService usuarioService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminUsuarioController(usuarioService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listsPendingRegistrationsWithoutCredentials() throws Exception {
        when(usuarioService.findPendentes()).thenReturn(List.of(new UsuarioPendenteSummaryResponse(
                USUARIO_ID,
                "Bia Estagiária",
                "bia@fen.br",
                "98765432100",
                Role.ESTAGIARIO,
                LocalDateTime.of(2026, 8, 21, 10, 30)
        )));

        mockMvc.perform(get("/api/admin/usuarios/pendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$[0].nome").value("Bia Estagiária"))
                .andExpect(jsonPath("$[0].email").value("bia@fen.br"))
                .andExpect(jsonPath("$[0].cpf").value("98765432100"))
                .andExpect(jsonPath("$[0].role").value("ESTAGIARIO"))
                .andExpect(jsonPath("$[0].solicitadoEm").value("2026-08-21T10:30:00"))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void returnsFullPendingDetailWithoutCredentials() throws Exception {
        when(usuarioService.findPendente(USUARIO_ID)).thenReturn(new UsuarioPendenteDetailResponse(
                USUARIO_ID,
                FUNCIONARIO_ID,
                "Bia Estagiária",
                "bia@fen.br",
                "98765432100",
                LocalDate.of(2002, 2, 3),
                Role.ESTAGIARIO,
                SituacaoUsuario.PENDENTE,
                LocalDateTime.of(2026, 8, 21, 10, 30),
                null,
                null,
                TipoEstagio.NAO_OBRIGATORIO,
                new SupervisorResponse(SUPERVISOR_ID, "Ana Supervisora"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 15)
        ));

        mockMvc.perform(get("/api/admin/usuarios/{id}", USUARIO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.funcionarioId").value(FUNCIONARIO_ID.toString()))
                .andExpect(jsonPath("$.nome").value("Bia Estagiária"))
                .andExpect(jsonPath("$.email").value("bia@fen.br"))
                .andExpect(jsonPath("$.cpf").value("98765432100"))
                .andExpect(jsonPath("$.dataNascimento").value("2002-02-03"))
                .andExpect(jsonPath("$.role").value("ESTAGIARIO"))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.tipoEstagio").value("NAO_OBRIGATORIO"))
                .andExpect(jsonPath("$.supervisor.id").value(SUPERVISOR_ID.toString()))
                .andExpect(jsonPath("$.supervisor.nome").value("Ana Supervisora"))
                .andExpect(jsonPath("$.inicioVigencia").value("2026-08-01"))
                .andExpect(jsonPath("$.fimVigencia").value("2026-12-15"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.hashSenha").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnknownRegistration() throws Exception {
        when(usuarioService.findPendente(USUARIO_ID))
                .thenThrow(new NoSuchElementException("Usuário não encontrado"));

        mockMvc.perform(get("/api/admin/usuarios/{id}", USUARIO_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));
    }

    @Test
    void approvesPendingRegistration() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", USUARIO_ID))
                .andExpect(status().isOk());

        verify(usuarioService).aprovar(USUARIO_ID);
    }

    @Test
    void returnsNotFoundWhenApprovingUnknownRegistration() throws Exception {
        doThrow(new NoSuchElementException("Usuário não encontrado"))
                .when(usuarioService).aprovar(USUARIO_ID);

        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", USUARIO_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsPendingRegistration() throws Exception {
        mockMvc.perform(delete("/api/admin/usuarios/{id}/rejeitar", USUARIO_ID))
                .andExpect(status().isNoContent());

        verify(usuarioService).rejeitar(USUARIO_ID);
    }

    @Test
    void declaresAdminAuthorizationForEveryEndpoint() {
        PreAuthorize authorization = AdminUsuarioController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasRole('ADMIN')");
    }
}
