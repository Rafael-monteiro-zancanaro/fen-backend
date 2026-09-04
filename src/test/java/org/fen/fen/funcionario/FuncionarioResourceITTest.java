package org.fen.fen.funcionario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:funcionarioresourceit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FuncionarioResourceITTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test void adminManagesTechnicalResponsibilityAndOtherRolesAreDenied() throws Exception {
        String admin = login("admin@fen.br", "admin123");
        var registration = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Farmacêutica IT\",\"cpf\":\"98765432100\",\"email\":\"farmaceutica.it@fen.br\",\"senha\":\"segredo123\",\"role\":\"FARMACEUTICO\",\"crf\":\"PR-999\"}"))
                .andExpect(status().isCreated()).andReturn();
        String usuarioId = objectMapper.readTree(registration.getResponse().getContentAsString()).get("usuarioId").asString();
        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", usuarioId).header(HttpHeaders.AUTHORIZATION, bearer(admin))).andExpect(status().isOk());
        String pharmacist = login("farmaceutica.it@fen.br", "segredo123");
        var list = mockMvc.perform(get("/api/admin/funcionarios").param("query", "farmaceutica.it").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray()).andReturn();
        JsonNode employee = objectMapper.readTree(list.getResponse().getContentAsString()).get("content").get(0);
        org.assertj.core.api.Assertions.assertThat(employee.get("usuarioId").asText()).isEqualTo(usuarioId);
        String id = employee.get("id").asString();
        var internRegistration = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Estagiária IT\",\"cpf\":\"12312312312\",\"email\":\"estagiaria.it@fen.br\",\"senha\":\"segredo123\",\"role\":\"ESTAGIARIO\",\"tipoEstagio\":\"OBRIGATORIO\",\"supervisorId\":\"" + id + "\",\"inicioVigencia\":\"2026-01-01\",\"fimVigencia\":\"2026-12-01\"}"))
                .andExpect(status().isCreated()).andReturn();
        String internUserId = objectMapper.readTree(internRegistration.getResponse().getContentAsString()).get("usuarioId").asString();
        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", internUserId).header(HttpHeaders.AUTHORIZATION, bearer(admin))).andExpect(status().isOk());
        String intern = login("estagiaria.it@fen.br", "segredo123");
        mockMvc.perform(get("/api/admin/funcionarios/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(admin))).andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/funcionarios/{id}/responsavel-tecnico", id).header(HttpHeaders.AUTHORIZATION, bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"responsavelTecnico\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.responsavelTecnico").value(true));
        mockMvc.perform(patch("/api/admin/funcionarios/{id}/responsavel-tecnico", id).header(HttpHeaders.AUTHORIZATION, bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"responsavelTecnico\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.responsavelTecnico").value(false));
        mockMvc.perform(get("/api/admin/funcionarios").header(HttpHeaders.AUTHORIZATION, bearer(pharmacist))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/funcionarios").header(HttpHeaders.AUTHORIZATION, bearer(intern))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", internUserId).header(HttpHeaders.AUTHORIZATION, bearer(pharmacist))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", internUserId).header(HttpHeaders.AUTHORIZATION, bearer(intern))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/usuarios/{id}/aprovar", internUserId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/funcionarios")).andExpect(status().isUnauthorized());
    }

    private String login(String email, String senha) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }
    private String bearer(String token) { return "Bearer " + token; }
}
