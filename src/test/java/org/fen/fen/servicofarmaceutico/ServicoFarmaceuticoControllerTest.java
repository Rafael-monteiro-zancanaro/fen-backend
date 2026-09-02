package org.fen.fen.servicofarmaceutico;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.fen.fen.service.ServicoFarmaceuticoService;
import org.fen.fen.controller.ServicoFarmaceuticoController;
import org.fen.fen.error.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServicoFarmaceuticoControllerTest {
    private MockMvc mockMvc;
    private ServicoFarmaceuticoService service;

    @BeforeEach void setup() {
        service = mock(ServicoFarmaceuticoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ServicoFarmaceuticoController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void rejeitaRequestDeCriacaoSemPaciente() throws Exception {
        mockMvc.perform(post("/api/servicos-farmaceuticos").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void encaminhaFiltroDeReconsultaHojeParaAListagem() throws Exception {
        when(service.listar(any(), any(), eq(true), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 10), 0
                ));

        mockMvc.perform(get("/api/servicos-farmaceuticos").param("retornoHoje", "true"))
                .andExpect(status().isOk());

        verify(service).listar(any(), any(), eq(true), any());
    }
}
