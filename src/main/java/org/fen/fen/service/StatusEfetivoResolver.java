package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class StatusEfetivoResolver {
    private final Clock clock;

    public StatusServicoFarmaceutico resolver(ServicoFarmaceutico atendimento) {
        if (atendimento.getStatusPersistido() == StatusServicoFarmaceutico.CONCLUIDO) return StatusServicoFarmaceutico.CONCLUIDO;
        if (atendimento.getAcompanhamento() != null
                && atendimento.getAcompanhamento().getProximoRetorno() != null
                && atendimento.getAcompanhamento().getProximoRetorno().isBefore(LocalDate.now(clock))) {
            return StatusServicoFarmaceutico.EXPIRADO;
        }
        return StatusServicoFarmaceutico.AGUARDANDO_RETORNO;
    }
}
