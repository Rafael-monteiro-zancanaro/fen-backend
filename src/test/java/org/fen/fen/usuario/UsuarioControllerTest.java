package org.fen.fen.usuario;

import org.fen.fen.controller.UsuarioController;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.error.ApiExceptionHandler;
import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.error.ConflictException;
import org.fen.fen.service.UsuarioService;
import org.fen.fen.domain.dto.SupervisorResponse;
import org.fen.fen.domain.dto.UsuarioRegisterResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID FUNCIONARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    private UsuarioService usuarioService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UsuarioController(usuarioService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void registersPendingUserWithoutExposingCredentials() throws Exception {
        when(usuarioService.register(any())).thenReturn(new UsuarioRegisterResponse(
                USUARIO_ID,
                FUNCIONARIO_ID,
                "nova@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.PENDENTE
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPharmacistJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.funcionarioId").value(FUNCIONARIO_ID.toString()))
                .andExpect(jsonPath("$.email").value("nova@fen.br"))
                .andExpect(jsonPath("$.role").value("FARMACEUTICO"))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.hashSenha").doesNotExist());
    }

    @Test
    void returnsFieldErrorForMalformedEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPharmacistJson().replace("nova@fen.br", "email-invalido")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void rejectsPasswordAboveBcryptUtf8ByteLimitBeforeRegistration() throws Exception {
        String password = "é".repeat(37);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPharmacistJson().replace("segredo123", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.senha").exists());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void acceptsEmailWithSurroundingWhitespaceThroughHttpValidation() throws Exception {
        when(usuarioService.register(any())).thenReturn(new UsuarioRegisterResponse(
                USUARIO_ID,
                FUNCIONARIO_ID,
                "nova@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.PENDENTE
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPharmacistJson().replace(
                                "nova@fen.br",
                                "  NOVA@FEN.BR  "
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nova@fen.br"));
    }

    @Test
    void returnsConflictForDuplicateRegistration() throws Exception {
        when(usuarioService.register(any())).thenThrow(new ConflictException("E-mail já cadastrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPharmacistJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    @Test
    void returnsConflictWhenDatabaseRejectsAConcurrentDuplicate() throws Exception {
        when(usuarioService.register(any()))
                .thenThrow(new DataIntegrityViolationException("unique constraint violation"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPharmacistJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("E-mail ou CPF já cadastrado"));
    }

    @Test
    void returnsBadRequestForBusinessRuleViolation() throws Exception {
        when(usuarioService.register(any()))
                .thenThrow(new BusinessRuleException("Cadastro público não permite a role ADMIN"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPharmacistJson().replace("FARMACEUTICO", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void listsOnlySupervisorIdAndName() throws Exception {
        UUID supervisorId = UUID.fromString("00000000-0000-0000-0000-000000000203");
        when(usuarioService.findSupervisoresAtivos())
                .thenReturn(List.of(new SupervisorResponse(supervisorId, "Ana Supervisora")));

        mockMvc.perform(get("/api/public/supervisores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(supervisorId.toString()))
                .andExpect(jsonPath("$[0].nome").value("Ana Supervisora"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].role").doesNotExist())
                .andExpect(jsonPath("$[0].situacao").doesNotExist());
    }

    private String validPharmacistJson() {
        return """
                {
                  "nome": "Nova Farmacêutica",
                  "cpf": "12345678901",
                  "dataNascimento": "1990-01-01",
                  "email": "nova@fen.br",
                  "senha": "segredo123",
                  "role": "FARMACEUTICO",
                  "crf": "PR-12345",
                  "responsavelTecnico": true
                }
                """;
    }
}
