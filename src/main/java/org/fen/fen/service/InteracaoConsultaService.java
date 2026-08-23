package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.comorbidade.dto.ComorbidadeSummaryResponse;
import org.fen.fen.interacao.dto.InteracaoResponse;
import org.fen.fen.mapper.MedicamentoMapper;
import org.fen.fen.repository.InteracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteracaoConsultaService {
    private final InteracaoRepository repository;
    private final MedicamentoMapper medicamentoMapper;

    @Transactional(readOnly = true)
    public List<InteracaoResponse> buscar(Set<UUID> medicamentoIds, Set<UUID> comorbidadeIds) {
        if (medicamentoIds.isEmpty() || comorbidadeIds.isEmpty()) return List.of();
        return repository.buscarPares(medicamentoIds, comorbidadeIds).stream().map(i ->
                new InteracaoResponse(i.getId(), medicamentoMapper.toSummary(i.getMedicamento()),
                        new ComorbidadeSummaryResponse(i.getComorbidade().getId(),
                                i.getComorbidade().getNome(), 0, i.getComorbidade().getCreatedAt()))).toList();
    }
}
