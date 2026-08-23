package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.domain.Medicamento;
import org.fen.fen.error.ConflictException;
import org.fen.fen.mapper.MedicamentoMapper;
import org.fen.fen.medicamento.dto.MedicamentoRequest;
import org.fen.fen.medicamento.dto.MedicamentoResponse;
import org.fen.fen.medicamento.dto.MedicamentoSummaryResponse;
import org.fen.fen.repository.MedicamentoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicamentoService {
    private final MedicamentoRepository repository;
    private final MedicamentoMapper mapper;

    @Transactional
    public MedicamentoResponse criar(MedicamentoRequest request) {
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public MedicamentoResponse buscarPorId(UUID id) {
        return mapper.toResponse(encontrar(id));
    }

    @Transactional(readOnly = true)
    public Page<MedicamentoResponse> listar(String query, Pageable pageable) {
        return repository.buscar(normalizar(query), pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MedicamentoSummaryResponse> autocomplete(String query, int limit) {
        String termo = normalizar(query);
        if (termo.isEmpty()) return List.of();
        return repository.autocomplete(termo, PageRequest.of(0, limit)).stream()
                .map(mapper::toSummary).toList();
    }

    @Transactional
    public MedicamentoResponse atualizar(UUID id, MedicamentoRequest request) {
        Medicamento entity = encontrar(id);
        mapper.update(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void excluir(UUID id) {
        encontrar(id);
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Medicamento possui referências e não pode ser excluído");
        }
    }

    private Medicamento encontrar(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new NoSuchElementException("Medicamento não encontrado: " + id));
    }

    private String normalizar(String value) {
        return value == null ? "" : value.trim();
    }
}
