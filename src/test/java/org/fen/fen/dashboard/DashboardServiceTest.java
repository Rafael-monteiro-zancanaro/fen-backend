package org.fen.fen.dashboard;

import org.fen.fen.dashboard.dto.DashboardResponse;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.fen.fen.repository.ServicoFarmaceuticoRepository;
import org.fen.fen.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final ServicoFarmaceuticoRepository repository = mock(ServicoFarmaceuticoRepository.class);
    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(
                repository,
                Clock.fixed(Instant.parse("2026-09-02T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void montaIndicadoresEContagensHistoricasComCategoriasAusentesComoZero() {
        when(repository.count()).thenReturn(8L);
        LocalDate today = LocalDate.of(2026, 9, 2);
        when(repository.contarAguardandoRetornoAtivoEmOrAfter(today)).thenReturn(2L);
        when(repository.contarReconsultasAtivasEm(today)).thenReturn(1L);
        when(repository.contarExpiradosAtivosAntesDe(today)).thenReturn(3L);
        when(repository.countByStatusPersistido(StatusServicoFarmaceutico.CONCLUIDO)).thenReturn(3L);
        when(repository.contarCuidadosFarmaceuticos()).thenReturn(4L);
        when(repository.contarAplicacoesInjetaveis()).thenReturn(2L);
        when(repository.contarInaloterapias()).thenReturn(1L);
        when(repository.contarServicosFarmaceuticos()).thenReturn(0L);

        DashboardResponse response = service.buscarResumo();

        assertThat(response.indicators())
                .isEqualTo(new DashboardResponse.Indicators(2L, 1L, 8L, 3L));
        assertThat(response.serviceTypes()).containsExactly(
                new DashboardResponse.ServiceTypeCount("cuidados-farmaceuticos", 4L),
                new DashboardResponse.ServiceTypeCount("aplicacao-injetaveis", 2L),
                new DashboardResponse.ServiceTypeCount("inaloterapia", 1L),
                new DashboardResponse.ServiceTypeCount("servicos-farmaceuticos", 0L)
        );
        assertThat(response.statuses()).containsExactly(
                new DashboardResponse.StatusCount(StatusServicoFarmaceutico.CONCLUIDO, 3L),
                new DashboardResponse.StatusCount(StatusServicoFarmaceutico.AGUARDANDO_RETORNO, 2L),
                new DashboardResponse.StatusCount(StatusServicoFarmaceutico.EXPIRADO, 3L)
        );
    }
}
