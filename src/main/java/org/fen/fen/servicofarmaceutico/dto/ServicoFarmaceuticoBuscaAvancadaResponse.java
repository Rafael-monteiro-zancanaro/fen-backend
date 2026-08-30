package org.fen.fen.servicofarmaceutico.dto;

import java.util.List;

public record ServicoFarmaceuticoBuscaAvancadaResponse(
        ServicoFarmaceuticoResumoResponse attendance,
        List<ServicoFarmaceuticoResponse.Medication> matchedMedications
) {
}
