package org.fen.fen.servicofarmaceutico;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.fen.fen.service.ServicoFarmaceuticoService;
import org.fen.fen.controller.ServicoFarmaceuticoController;
import org.fen.fen.error.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServicoFarmaceuticoControllerTest {
    private MockMvc mockMvc;

    @BeforeEach void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ServicoFarmaceuticoController(mock(ServicoFarmaceuticoService.class)))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void rejeitaRequestDeCriacaoSemPaciente() throws Exception {
        mockMvc.perform(post("/api/servicos-farmaceuticos").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
