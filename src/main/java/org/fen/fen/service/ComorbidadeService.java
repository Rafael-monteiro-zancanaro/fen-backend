package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.comorbidade.dto.ComorbidadeRequest;
import org.fen.fen.comorbidade.dto.ComorbidadeResponse;
import org.fen.fen.comorbidade.dto.ComorbidadeSummaryResponse;
import org.fen.fen.domain.Comorbidade;
import org.fen.fen.domain.Medicamento;
import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.error.ConflictException;
import org.fen.fen.mapper.ComorbidadeMapper;
import org.fen.fen.repository.ComorbidadeRepository;
import org.fen.fen.repository.MedicamentoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComorbidadeService {
    private final ComorbidadeRepository repository;
    private final MedicamentoRepository medicamentoRepository;
    private final ComorbidadeMapper mapper;

    @Transactional
    public ComorbidadeResponse criar(ComorbidadeRequest request) {
        Comorbidade entity = new Comorbidade(request.name().trim());
        medicamentos(request.medicationInteractionIds()).forEach(entity::adicionarInteracao);
        return mapper.toResponse(salvar(entity));
    }

    @Transactional(readOnly = true)
    public ComorbidadeResponse buscarPorId(UUID id) {
        return mapper.toResponse(encontrarDetalhe(id));
    }

    @Transactional(readOnly = true)
    public Page<ComorbidadeSummaryResponse> listar(String query, Pageable pageable) {
        return repository.buscar(query == null ? "" : query.trim(), pageable).map(item ->
                new ComorbidadeSummaryResponse(item.getId(), item.getNome(),
                        item.getInteractionCount(), item.getCreatedAt()));
    }

    @Transactional
    public ComorbidadeResponse atualizar(UUID id, ComorbidadeRequest request) {
        Comorbidade entity = encontrarDetalhe(id);
        entity.setNome(request.name().trim());
        Set<UUID> desejados = idsUnicos(request.medicationInteractionIds());
        Set<UUID> atuais = entity.getInteracoes().stream()
                .map(i -> i.getMedicamento().getId()).collect(Collectors.toSet());
        atuais.stream().filter(current -> !desejados.contains(current))
                .toList().forEach(entity::removerInteracao);
        List<UUID> novosIds = desejados.stream().filter(idNovo -> !atuais.contains(idNovo)).toList();
        medicamentos(novosIds).forEach(entity::adicionarInteracao);
        return mapper.toResponse(salvar(entity));
    }

    @Transactional
    public void excluir(UUID id) {
        encontrarDetalhe(id);
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Comorbidade possui referências e não pode ser excluída");
        }
    }

    private Comorbidade encontrarDetalhe(UUID id) {
        return repository.buscarDetalhe(id).orElseThrow(() ->
                new NoSuchElementException("Comorbidade não encontrada: " + id));
    }

    private List<Medicamento> medicamentos(List<UUID> ids) {
        Set<UUID> unicos = idsUnicos(ids);
        List<Medicamento> encontrados = medicamentoRepository.findAllById(unicos);
        if (encontrados.size() != unicos.size()) {
            throw new NoSuchElementException("Um ou mais medicamentos não foram encontrados");
        }
        return encontrados;
    }

    private Set<UUID> idsUnicos(List<UUID> ids) {
        Set<UUID> unicos = new LinkedHashSet<>(ids);
        if (unicos.size() != ids.size()) {
            throw new BusinessRuleException("Não é permitido repetir medicamento na interação");
        }
        return unicos;
    }

    private Comorbidade salvar(Comorbidade entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Interação entre comorbidade e medicamento já cadastrada");
        }
    }
}
