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

import java.util.concurrent.atomic.AtomicInteger;

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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
