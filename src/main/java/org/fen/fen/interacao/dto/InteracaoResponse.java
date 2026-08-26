package org.fen.fen.interacao.dto;

import org.fen.fen.comorbidade.dto.ComorbidadeSummaryResponse;
import org.fen.fen.medicamento.dto.MedicamentoSummaryResponse;

import java.util.UUID;

public record InteracaoResponse(
        UUID id,
        MedicamentoSummaryResponse medication,
        ComorbidadeSummaryResponse comorbidity
) {}
