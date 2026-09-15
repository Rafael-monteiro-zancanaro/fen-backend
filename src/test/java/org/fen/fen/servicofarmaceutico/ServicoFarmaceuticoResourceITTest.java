package org.fen.fen.servicofarmaceutico;

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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:servicofarmaceuticoit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServicoFarmaceuticoResourceITTest {

    private static final AtomicInteger PATIENT_SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void criaListaVisualizaEConcluiSequenciaDeRetorno() throws Exception {
        String token = login();
        String patientId = criarPaciente(token);

        MvcResult criado = mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"followUp\":{\"returnIntervalDays\":1,\"returnCount\":1}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_RETORNO"))
                .andExpect(jsonPath("$.followUpProgress.nextReturnNumber").value(1))
                .andReturn();
        String initialId = field(criado, "id");

        mockMvc.perform(get("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("status", "AGUARDANDO_RETORNO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(initialId));

        mockMvc.perform(put("/api/servicos-farmaceuticos/{id}", initialId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"care\":{\"bloodGlucose\":90},"
                                + "\"followUp\":{\"returnIntervalDays\":1,\"returnCount\":1}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.care.bloodGlucose").value("90"));

        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}/continuacao", initialId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient.id").value(patientId));

        MvcResult retorno = mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", initialId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.patient.id").value(patientId))
                .andExpect(jsonPath("$.followUpLink.originAttendanceId").value(initialId))
                .andExpect(jsonPath("$.followUp.returnCount").value(1))
                .andExpect(jsonPath("$.followUpProgress.canContinue").value(false))
                .andReturn();
        String returnId = field(retorno, "id");

        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}", returnId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followUpHistory.length()").value(2));

        mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", initialId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void prolongaAcompanhamentoNoUltimoRetornoSemCriarNovaCadeia() throws Exception {
        String token = login();
        String patientId = criarPaciente(token);
        MvcResult inicial = mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId
                                + "\",\"followUp\":{\"returnIntervalDays\":7,\"returnCount\":3}}"))
                .andExpect(status().isCreated())
                .andReturn();
        String chainId = nestedField(inicial, "followUpLink", "chainId");

        MvcResult primeiroRetorno = mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", field(inicial, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_RETORNO"))
                .andReturn();

        MvcResult segundoRetorno = mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", field(primeiroRetorno, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_RETORNO"))
                .andReturn();

        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}/continuacao", field(segundoRetorno, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canExtendFollowUp").value(true));

        MvcResult ultimoRetorno = mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", field(segundoRetorno, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"followUpExtension\":{"
                                + "\"additionalReturns\":2,\"returnIntervalDays\":7}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_RETORNO"))
                .andExpect(jsonPath("$.followUp.returnCount").value(5))
                .andExpect(jsonPath("$.followUpProgress.completedReturns").value(3))
                .andExpect(jsonPath("$.followUpProgress.nextReturnNumber").value(4))
                .andReturn();

        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}", field(ultimoRetorno, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followUpLink.originAttendanceId").value(field(inicial, "id")))
                .andExpect(jsonPath("$.followUpLink.chainId").value(chainId));

        mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", field(segundoRetorno, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"followUpExtension\":{"
                                + "\"additionalReturns\":2,\"returnIntervalDays\":7}}"))
                .andExpect(status().isConflict());

        MvcResult quartoRetorno = mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", field(ultimoRetorno, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_RETORNO"))
                .andExpect(jsonPath("$.followUpProgress.nextReturnNumber").value(5))
                .andReturn();

        mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", field(quartoRetorno, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.followUpProgress.canContinue").value(false));
    }

    @Test
    void exigeAutenticacaoParaListagem() throws Exception {
        mockMvc.perform(get("/api/servicos-farmaceuticos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaAcompanhamentoAninhadoAoCriarRetorno() throws Exception {
        String token = login();
        String patientId = criarPaciente(token);
        MvcResult inicial = mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId
                                + "\",\"followUp\":{\"returnIntervalDays\":7,\"returnCount\":2}}"))
                .andExpect(status().isCreated())
                .andReturn();
        String initialId = field(inicial, "id");

        mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", initialId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId
                                + "\",\"followUp\":{\"returnIntervalDays\":1,\"returnCount\":1}}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}", initialId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followUp.returnIntervalDays").value(7))
                .andExpect(jsonPath("$.followUp.returnCount").value(2))
                .andExpect(jsonPath("$.followUpHistory.length()").value(1));
    }

    @Test
    void rejeitaExtensaoForaDoUltimoRetornoEComQuantidadeInvalida() throws Exception {
        String token = login();
        String patientId = criarPaciente(token);
        MvcResult inicial = mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId
                                + "\",\"followUp\":{\"returnIntervalDays\":7,\"returnCount\":2}}"))
                .andExpect(status().isCreated())
                .andReturn();
        String initialId = field(inicial, "id");

        mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"followUpExtension\":{"
                                + "\"additionalReturns\":1,\"returnIntervalDays\":7}}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", initialId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"followUpExtension\":{"
                                + "\"additionalReturns\":1,\"returnIntervalDays\":7}}"))
                .andExpect(status().isBadRequest());

        MvcResult primeiroRetorno = mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", initialId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        for (int additionalReturns : new int[]{0, -1}) {
            mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", field(primeiroRetorno, "id"))
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"patientId\":\"" + patientId + "\",\"followUpExtension\":{"
                                    + "\"additionalReturns\":" + additionalReturns + ",\"returnIntervalDays\":7}}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void permiteApenasUmaExtensaoConcorrenteDoMesmoUltimoRetorno() throws Exception {
        String token = login();
        String patientId = criarPaciente(token);
        MvcResult inicial = mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId
                                + "\",\"followUp\":{\"returnIntervalDays\":7,\"returnCount\":2}}"))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult primeiroRetorno = mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", field(inicial, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var attempts = List.of(
                    executor.submit(() -> criarExtensaoConcorrente(
                            field(primeiroRetorno, "id"), patientId, token, ready, start
                    )),
                    executor.submit(() -> criarExtensaoConcorrente(
                            field(primeiroRetorno, "id"), patientId, token, ready, start
                    ))
            );
            ready.await();
            start.countDown();

            assertThat(List.of(attempts.get(0).get(), attempts.get(1).get()))
                    .containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void persistePrescritoresPorItemEUsaViaDoCadastroDoMedicamento() throws Exception {
        String token = login();
        String patientId = criarPaciente(token);
        String injectableMedicationId = criarMedicamento(token, "Dipirona", "Intramuscular");
        String inhalotherapyMedicationId = criarMedicamento(token, "Salbutamol", "Inalatória");

        MvcResult created = mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"%s",
                                  "injectable":{"medications":[
                                    {"medicationId":"%s","batch":"INJ-01","expirationDate":"2027-01-01",
                                     "dosage":"1 ampola","prescriberName":"Dr. João","prescriberRegistration":"CRM 12345"},
                                    {"medicationId":"%s","batch":"INJ-02","expirationDate":"2027-02-01",
                                     "dosage":"2 ampolas","prescriberName":"Dra. Maria","prescriberRegistration":"CRO 67890"}
                                  ]},
                                  "inhalotherapy":{"medications":[
                                    {"medicationId":"%s","batch":"INA-01","expirationDate":"2027-03-01",
                                     "dosage":"2 jatos","prescriberName":"Dr. Pedro","prescriberRegistration":"CRM 98765"}
                                  ]}
                                }
                                """.formatted(
                                patientId,
                                injectableMedicationId,
                                injectableMedicationId,
                                inhalotherapyMedicationId
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.injectable.medications.length()").value(2))
                .andExpect(jsonPath("$.injectable.medications[0].prescriberName").value("Dr. João"))
                .andExpect(jsonPath("$.injectable.medications[0].prescriberRegistration").value("CRM 12345"))
                .andExpect(jsonPath("$.injectable.medications[0].administrationRoute").value("Intramuscular"))
                .andExpect(jsonPath("$.injectable.medications[1].prescriberName").value("Dra. Maria"))
                .andExpect(jsonPath("$.injectable.medications[1].prescriberRegistration").value("CRO 67890"))
                .andExpect(jsonPath("$.inhalotherapy.medications[0].prescriberName").value("Dr. Pedro"))
                .andExpect(jsonPath("$.inhalotherapy.medications[0].prescriberRegistration").value("CRM 98765"))
                .andExpect(jsonPath("$.inhalotherapy.medications[0].administrationRoute").value("Inalatória"))
                .andReturn();

        mockMvc.perform(get("/api/servicos-farmaceuticos/{id}", field(created, "id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.injectable.medications[1].prescriberName").value("Dra. Maria"))
                .andExpect(jsonPath("$.inhalotherapy.medications[0].prescriberRegistration").value("CRM 98765"));
    }

    @Test
    void naoPersisteNemExpõeCamposObsoletosDoStepDeServicosFarmaceuticos() throws Exception {
        String token = login();
        String patientId = criarPaciente(token);
        String medicationId = criarMedicamento(token, "Paracetamol", "Oral");

        mockMvc.perform(post("/api/servicos-farmaceuticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"%s",
                                  "complementaryServices":{
                                    "homeCare":false,
                                    "pharmacotherapeuticFollowUp":true,
                                    "minorDisorderIndication":false,
                                    "signsAndSymptoms":"Uso contínuo",
                                    "medications":[
                                      {"medicationId":"%s","batch":"CMP-01","expirationDate":"2027-04-01","dosage":"1 comprimido"}
                                    ],
                                    "recordNumber":"F-001",
                                    "attendanceDate":"2026-08-30"
                                  }
                                }
                                """.formatted(patientId, medicationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.complementaryServices.recordNumber").doesNotExist())
                .andExpect(jsonPath("$.complementaryServices.attendanceDate").doesNotExist());
    }

    private String criarPaciente(String token) throws Exception {
        String cpf = String.format("1234567%04d", PATIENT_SEQUENCE.incrementAndGet());
        MvcResult result = mockMvc.perform(post("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maria da Silva","cpf":"%s","birthDate":"1990-01-01",
                                "cellPhone":"44999999999","gender":"feminino","address":"Rua A","city":"Maringá",
                                "state":"PR","phone":"","responsibleName":"","comorbidityIds":[]}
                                """.formatted(cpf)))
                .andExpect(status().isCreated())
                .andReturn();
        return field(result, "id");
    }

    private int criarExtensaoConcorrente(
            String previousAttendanceId,
            String patientId,
            String token,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();
        return mockMvc.perform(post("/api/servicos-farmaceuticos/{id}/retornos", previousAttendanceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"followUpExtension\":{"
                                + "\"additionalReturns\":1,\"returnIntervalDays\":7}}"))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private String criarMedicamento(String token, String name, String administrationRoute) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/medicamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","measurementUnit":"500 mg","administrationRoute":"%s"}
                                """.formatted(name, administrationRoute)))
                .andExpect(status().isCreated())
                .andReturn();
        return field(result, "id");
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fen.br\",\"senha\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return field(result, "token");
    }

    private String field(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(field).asString();
    }

    private String nestedField(MvcResult result, String parent, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(parent).get(field).asString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
