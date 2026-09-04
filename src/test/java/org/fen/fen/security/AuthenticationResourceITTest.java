package org.fen.fen.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:authenticationresourceit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AuthenticationResourceITTest {

    private static final String ADMIN_EMAIL = "admin@fen.br";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String PHARMACIST_EMAIL = "integration.pharmacist@fen.br";
    private static final String PHARMACIST_PASSWORD = "integration-secret";
    private static final String WRONG_PASSWORD = "incorrect-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void provesRegistrationApprovalAuthenticationAndAuthorizationBoundaries(
            CapturedOutput output
    ) throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pharmacistRegistration(
                                "Farmacêutica de Integração",
                                "12345678901",
                                PHARMACIST_EMAIL,
                                PHARMACIST_PASSWORD,
                                "PR-12345"
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(PHARMACIST_EMAIL))
                .andExpect(jsonPath("$.role").value("FARMACEUTICO"))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        UUID pharmacistId = UUID.fromString(json(registration).get("usuarioId").asString());

        assertInvalidLogin(PHARMACIST_EMAIL, PHARMACIST_PASSWORD);
        assertInvalidLogin(PHARMACIST_EMAIL, WRONG_PASSWORD);
        assertInvalidLogin("unknown.integration@fen.br", PHARMACIST_PASSWORD);

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD, "ADMIN");

        MvcResult pendingListResult = mockMvc.perform(get("/api/admin/usuarios/pendentes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pendingSummary = findById(json(pendingListResult), pharmacistId);
        assertThat(pendingSummary.get("email").asString()).isEqualTo(PHARMACIST_EMAIL);
        assertThat(pendingSummary.get("role").asString()).isEqualTo("FARMACEUTICO");
        assertThat(pendingSummary.has("passwordHash")).isFalse();

        mockMvc.perform(get("/api/admin/usuarios/{id}", pharmacistId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pharmacistId.toString()))
                .andExpect(jsonPath("$.nome").value("Farmacêutica de Integração"))
                .andExpect(jsonPath("$.email").value(PHARMACIST_EMAIL))
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.role").value("FARMACEUTICO"))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.crf").value("PR-12345"))
                .andExpect(jsonPath("$.responsavelTecnico").value(false))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", pharmacistId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", pharmacistId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        String pharmacistToken = login(
                PHARMACIST_EMAIL,
                PHARMACIST_PASSWORD,
                "FARMACEUTICO"
        );

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(pharmacistToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pharmacistId.toString()))
                .andExpect(jsonPath("$.email").value(PHARMACIST_EMAIL))
                .andExpect(jsonPath("$.role").value("FARMACEUTICO"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(get("/api/admin/usuarios/pendentes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(pharmacistToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Acesso negado"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticação necessária"));

        assertThat(output.getAll())
                .doesNotContain(ADMIN_PASSWORD)
                .doesNotContain(PHARMACIST_PASSWORD)
                .doesNotContain(WRONG_PASSWORD)
                .doesNotContain(adminToken)
                .doesNotContain(pharmacistToken);
    }

    @Test
    void rejectionDeletesPersistedRegistrationAndReleasesEmailAndCpfConstraints() throws Exception {
        String registration = pharmacistRegistration(
                "Farmacêutica Rejeitada",
                "98765432100",
                "rejected.integration@fen.br",
                "rejected-secret",
                "PR-98765"
        );
        MvcResult firstRegistration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode firstBody = json(firstRegistration);

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD, "ADMIN");
        mockMvc.perform(delete("/api/admin/usuarios/{id}/rejeitar", firstBody.get("usuarioId").asString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(
                        org.hamcrest.Matchers.not(firstBody.get("usuarioId").asString())
                ))
                .andExpect(jsonPath("$.funcionarioId").value(
                        org.hamcrest.Matchers.not(firstBody.get("funcionarioId").asString())
                ))
                .andExpect(jsonPath("$.email").value("rejected.integration@fen.br"))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"));
    }

    private void assertInvalidLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(email, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    private String login(String email, String password, String expectedRole) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value(expectedRole))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();
        return json(login).get("token").asString();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode findById(JsonNode array, UUID id) {
        for (JsonNode item : array) {
            if (id.toString().equals(item.get("id").asString())) {
                return item;
            }
        }
        throw new AssertionError("Pending registration not found: " + id);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String loginRequest(String email, String password) {
        return """
                {
                  "email": "%s",
                  "senha": "%s"
                }
                """.formatted(email, password);
    }

    private String pharmacistRegistration(
            String name,
            String cpf,
            String email,
            String password,
            String crf
    ) {
        return """
                {
                  "nome": "%s",
                  "cpf": "%s",
                  "dataNascimento": "1990-01-01",
                  "email": "%s",
                  "senha": "%s",
                  "role": "FARMACEUTICO",
                  "crf": "%s"
                }
                """.formatted(name, cpf, email, password, crf);
    }
}
