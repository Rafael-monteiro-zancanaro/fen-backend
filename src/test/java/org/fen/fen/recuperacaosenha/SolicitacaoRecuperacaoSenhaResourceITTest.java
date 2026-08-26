package org.fen.fen.recuperacaosenha;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:recuperacaoit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SolicitacaoRecuperacaoSenhaResourceITTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void alteraSenhaSomenteDepoisDaAprovacaoAdministrativa() throws Exception {
        String adminToken = login("admin123");
        MvcResult created = mockMvc.perform(post("/api/recuperacoes-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fen.br\",\"novaSenha\":\"novaSenha123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.novaSenha").doesNotExist())
                .andExpect(jsonPath("$.novaSenhaHash").doesNotExist())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asString();

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fen.br\",\"senha\":\"novaSenha123\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/recuperacoes-senha/{id}/aprovar", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fen.br\",\"senha\":\"admin123\"}"))
                .andExpect(status().isUnauthorized());
        login("novaSenha123");
    }

    private String login(String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fen.br\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }
    private String bearer(String token) { return "Bearer " + token; }
}
