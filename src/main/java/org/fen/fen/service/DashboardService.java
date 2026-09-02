package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.dashboard.dto.DashboardResponse;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.fen.fen.repository.ServicoFarmaceuticoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ServicoFarmaceuticoRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse buscarResumo() {
        LocalDate today = LocalDate.now(clock);
        long awaitingReturn = repository.contarAguardandoRetornoAtivoEmOrAfter(today);
        long expired = repository.contarExpiradosAtivosAntesDe(today);

        return new DashboardResponse(
                new DashboardResponse.Indicators(
                        awaitingReturn,
                        repository.contarReconsultasAtivasEm(today),
                        repository.count(),
                        expired
                ),
                List.of(
                        new DashboardResponse.ServiceTypeCount(
                                "cuidados-farmaceuticos", repository.contarCuidadosFarmaceuticos()),
                        new DashboardResponse.ServiceTypeCount(
                                "aplicacao-injetaveis", repository.contarAplicacoesInjetaveis()),
                        new DashboardResponse.ServiceTypeCount("inaloterapia", repository.contarInaloterapias()),
                        new DashboardResponse.ServiceTypeCount(
                                "servicos-farmaceuticos", repository.contarServicosFarmaceuticos())
                ),
                List.of(
                        new DashboardResponse.StatusCount(
                                StatusServicoFarmaceutico.CONCLUIDO,
                                repository.countByStatusPersistido(StatusServicoFarmaceutico.CONCLUIDO)
                        ),
                        new DashboardResponse.StatusCount(
                                StatusServicoFarmaceutico.AGUARDANDO_RETORNO, awaitingReturn
                        ),
                        new DashboardResponse.StatusCount(StatusServicoFarmaceutico.EXPIRADO, expired)
                )
        );
    }
}
