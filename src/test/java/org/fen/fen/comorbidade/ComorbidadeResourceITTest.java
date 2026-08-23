package org.fen.fen.comorbidade;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:comorbidadeit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComorbidadeResourceITTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void cadastraDoisMedicamentosERemoveSomenteUmaInteracao() throws Exception {
        String token = login();
        String a = criarMedicamento(token, "Dipirona");
        String b = criarMedicamento(token, "Ibuprofeno");
        String comorbidade = criarComorbidade(token, a, b);

        mockMvc.perform(get("/api/comorbidades/{id}", comorbidade).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.interactionMedications.length()").value(2));

        mockMvc.perform(put("/api/comorbidades/{id}", comorbidade)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hipertensão\",\"medicationInteractionIds\":[\"" + a + "\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.medicationInteractionIds.length()").value(1))
                .andExpect(jsonPath("$.medicationInteractionIds[0]").value(a));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fen.br\",\"senha\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }

    private String criarMedicamento(String token, String nome) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/medicamentos").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + nome
                                + "\",\"measurementUnit\":\"mg\",\"administrationRoute\":\"Oral\"}"))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private String criarComorbidade(String token, String a, String b) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/comorbidades").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hipertensão\","
                                + "\"medicationInteractionIds\":[\"" + a + "\",\"" + b + "\"]}"))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
