package org.fen.fen.servicofarmaceutico;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:anexoatendimentoit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnexoAtendimentoResourceITTest {

    private static final AtomicInteger PATIENT_SEQUENCE = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void anexaListaBaixaERemoveArquivoDoAtendimentoCorreto() throws Exception {
        String token = login();
        String attendanceId = criarAtendimento(token);
        MockMultipartFile file = new MockMultipartFile(
                "file", "receita.pdf", "application/pdf", "conteúdo seguro".getBytes()
        );

        MvcResult uploaded = mockMvc.perform(multipart("/api/servicos-farmaceuticos/{id}/anexos", attendanceId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeOriginal").value("receita.pdf"))
                .andExpect(jsonPath("$.tamanho").value(16))
                .andReturn();
        String attachmentId = field(uploaded, "id");

        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}/anexos", attendanceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}/anexos/{attachmentId}", attendanceId, attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mockMvc.perform(delete("/api/servicos-farmaceuticos/{id}/anexos/{attachmentId}", attendanceId, attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}/anexos", attendanceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void exigeAutenticacaoParaListarAnexos() throws Exception {
        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}/anexos", java.util.UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private String criarAtendimento(String token) throws Exception {
        String cpf = String.format("1234567%04d", PATIENT_SEQUENCE.incrementAndGet());
        MvcResult patient = mockMvc.perform(post("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType("application/json")
                        .content("""
                                {"name":"Maria da Silva","cpf":"%s","birthDate":"1990-01-01",
                                "cellPhone":"44999999999","gender":"feminino","address":"Rua A","city":"Maringá",
                                "state":"PR","phone":"","responsibleName":"","comorbidityIds":[]}
                                """.formatted(cpf)))
                .andExpect(status().isCreated()).andReturn();
        String patientId = field(patient, "id");
        MvcResult attendance = mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType("application/json")
                        .content("{\"patientId\":\"%s\"}".formatted(patientId)))
                .andExpect(status().isCreated()).andReturn();
        return field(attendance, "id");
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"admin@fen.br\",\"senha\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn();
        return field(result, "token");
    }

    private String field(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(field).asString();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
