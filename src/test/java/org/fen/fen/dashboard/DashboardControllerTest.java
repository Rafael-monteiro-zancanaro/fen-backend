package org.fen.fen.dashboard;

import org.fen.fen.controller.DashboardController;
import org.fen.fen.dashboard.dto.DashboardResponse;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.fen.fen.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    private final DashboardService service = mock(DashboardService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(service)).build();
    }

    @Test
    void retornaResumoAgregadoDoDashboard() throws Exception {
        when(service.buscarResumo()).thenReturn(new DashboardResponse(
                new DashboardResponse.Indicators(2L, 1L, 8L, 3L),
                List.of(new DashboardResponse.ServiceTypeCount("cuidados-farmaceuticos", 4L)),
                List.of(new DashboardResponse.StatusCount(StatusServicoFarmaceutico.EXPIRADO, 3L))
        ));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicators.awaitingReturn").value(2))
                .andExpect(jsonPath("$.indicators.returnsToday").value(1))
                .andExpect(jsonPath("$.indicators.totalAttendances").value(8))
                .andExpect(jsonPath("$.indicators.expired").value(3))
                .andExpect(jsonPath("$.serviceTypes[0].type").value("cuidados-farmaceuticos"))
                .andExpect(jsonPath("$.statuses[0].status").value("EXPIRADO"));
    }
}
