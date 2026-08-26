package org.fen.fen.paciente;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pacienteit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class PacienteResourceITTest {
 @Autowired MockMvc mockMvc; @Autowired ObjectMapper objectMapper;
 @Test void cadastraBuscaPorCpfEAtualizaComorbidades() throws Exception {
  String token = login(); String a = comorbidade(token, "Diabetes"); String b = comorbidade(token, "Hipertensão");
  MvcResult result = mockMvc.perform(post("/api/pacientes").header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON).content(payload(a)))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.cpf").value("12345678901")).andExpect(jsonPath("$.comorbidityIds.length()").value(1)).andReturn();
  String paciente = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
  mockMvc.perform(get("/api/pacientes/cpf/123.456.789-01").header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(paciente));
  mockMvc.perform(put("/api/pacientes/{id}", paciente).header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON).content(payload(b)))
    .andExpect(status().isOk()).andExpect(jsonPath("$.comorbidityIds[0]").value(b));
 }
 private String comorbidade(String token,String nome) throws Exception { MvcResult r=mockMvc.perform(post("/api/comorbidades").header(HttpHeaders.AUTHORIZATION,bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+nome+"\",\"medicationInteractionIds\":[]}" )).andExpect(status().isCreated()).andReturn(); return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asString(); }
 private String payload(String comorbidade){return "{\"name\":\"Maria\",\"cpf\":\"123.456.789-01\",\"birthDate\":\"1990-01-01\",\"cellPhone\":\"(44) 99999-9999\",\"gender\":\"feminino\",\"cep\":\"87000-000\",\"address\":\"Rua A\",\"neighborhood\":\"Centro\",\"city\":\"Maringá\",\"state\":\"PR\",\"phone\":\"\",\"responsibleName\":\"\",\"comorbidityIds\":[\""+comorbidade+"\"]}";}
 private String login() throws Exception {MvcResult r=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin@fen.br\",\"senha\":\"admin123\"}")).andExpect(status().isOk()).andReturn();return objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asString();}
 private String bearer(String token){return "Bearer "+token;}
}
