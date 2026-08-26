package org.fen.fen.controller;

import org.fen.fen.service.ComorbidadeService;
import org.fen.fen.service.MedicamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CadastrosControllerContractTest {

    @Test
    void medicamentoInvalidoRetorna400ComErroNoCampoNome() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new MedicamentoController(mock(MedicamentoService.class)))
                .setControllerAdvice(new org.fen.fen.error.ApiExceptionHandler()).build();

        mvc.perform(post("/api/medicamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"measurementUnit\":\"mg\",\"administrationRoute\":\"Oral\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void comorbidadeInvalidaRetorna400() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new ComorbidadeController(mock(ComorbidadeService.class)))
                .setControllerAdvice(new org.fen.fen.error.ApiExceptionHandler()).build();
        mvc.perform(post("/api/comorbidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"medicationInteractionIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
