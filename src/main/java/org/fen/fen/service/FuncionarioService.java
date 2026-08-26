package org.fen.fen.service;

import java.util.NoSuchElementException;
import java.util.UUID;
import org.fen.fen.domain.Funcionario;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.dto.FuncionarioDetailResponse;
import org.fen.fen.domain.dto.FuncionarioSummaryResponse;
import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.mapper.FuncionarioMapper;
import org.fen.fen.repository.FuncionarioRepository;
import org.fen.fen.infra.AuditableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncionarioService {
    private final FuncionarioRepository repository;
    private final FuncionarioMapper mapper;

    public FuncionarioService(FuncionarioRepository repository, FuncionarioMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<FuncionarioSummaryResponse> listar(String query, Pageable pageable) {
        return repository.buscar(query.trim(), pageable).map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public FuncionarioDetailResponse buscar(UUID id) {
        return mapper.toDetail(find(id));
    }

    @Transactional
    public FuncionarioDetailResponse alterarResponsavelTecnico(UUID id, boolean responsavelTecnico) {
        Funcionario funcionario = find(id);
        if (funcionario.getUsuario().getRole() == Role.ESTAGIARIO
                || funcionario.getCrf() == null || funcionario.getCrf().isBlank()) {
            throw new BusinessRuleException("Funcionário não é farmacêutico");
        }
        funcionario.setUpdatedBy(AuditableEntity.currentAuditor());
        funcionario.setResponsavelTecnico(responsavelTecnico);
        return mapper.toDetail(repository.save(funcionario));
    }

    private Funcionario find(UUID id) {
        return repository.findDetailById(id)
                .orElseThrow(() -> new NoSuchElementException("Funcionário não encontrado"));
    }
}
