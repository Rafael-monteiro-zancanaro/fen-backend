package org.fen.fen.mapper;

import org.fen.fen.domain.Medicamento;
import org.fen.fen.medicamento.dto.MedicamentoRequest;
import org.fen.fen.medicamento.dto.MedicamentoResponse;
import org.fen.fen.medicamento.dto.MedicamentoSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class MedicamentoMapper {
    public Medicamento toEntity(MedicamentoRequest request) {
        return new Medicamento(request.name().trim(), request.measurementUnit().trim(),
                request.administrationRoute().trim());
    }

    public void update(Medicamento entity, MedicamentoRequest request) {
        entity.setNome(request.name().trim());
        entity.setUnidadeMedida(request.measurementUnit().trim());
        entity.setViaAdministracao(request.administrationRoute().trim());
    }

    public MedicamentoResponse toResponse(Medicamento entity) {
        return new MedicamentoResponse(entity.getId(), entity.getNome(), entity.getUnidadeMedida(),
                entity.getViaAdministracao(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public MedicamentoSummaryResponse toSummary(Medicamento entity) {
        return new MedicamentoSummaryResponse(entity.getId(), entity.getNome(),
                entity.getUnidadeMedida(), entity.getViaAdministracao());
    }
}
