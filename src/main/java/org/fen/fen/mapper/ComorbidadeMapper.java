package org.fen.fen.mapper;

import lombok.RequiredArgsConstructor;
import org.fen.fen.comorbidade.dto.ComorbidadeResponse;
import org.fen.fen.comorbidade.dto.ComorbidadeSummaryResponse;
import org.fen.fen.domain.Comorbidade;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComorbidadeMapper {
    private final MedicamentoMapper medicamentoMapper;

    public ComorbidadeResponse toResponse(Comorbidade entity) {
        var medications = entity.getInteracoes().stream()
                .map(interacao -> medicamentoMapper.toSummary(interacao.getMedicamento())).toList();
        return new ComorbidadeResponse(entity.getId(), entity.getNome(),
                medications.stream().map(item -> item.id()).toList(), medications,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public ComorbidadeSummaryResponse toSummary(Comorbidade entity) {
        return new ComorbidadeSummaryResponse(entity.getId(), entity.getNome(),
                entity.getInteracoes().size(), entity.getCreatedAt());
    }
}
