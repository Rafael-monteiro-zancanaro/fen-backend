package org.fen.fen.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.fen.fen.domain.dto.FuncionarioDetailResponse;
import org.fen.fen.domain.dto.FuncionarioSummaryResponse;
import org.fen.fen.domain.dto.ResponsavelTecnicoRequest;
import org.fen.fen.service.FuncionarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/funcionarios")
@PreAuthorize("hasRole('ADMIN')")
public class FuncionarioController {
    private final FuncionarioService service;

    public FuncionarioController(FuncionarioService service) { this.service = service; }

    @GetMapping
    public Page<FuncionarioSummaryResponse> listar(@RequestParam(defaultValue = "") String query,
                                                    @PageableDefault(size = 10) Pageable pageable) {
        return service.listar(query, pageable);
    }

    @GetMapping("/{id}")
    public FuncionarioDetailResponse buscar(@PathVariable UUID id) { return service.buscar(id); }

    @PatchMapping("/{id}/responsavel-tecnico")
    public FuncionarioDetailResponse alterarResponsavelTecnico(@PathVariable UUID id,
                                                                @Valid @RequestBody ResponsavelTecnicoRequest request) {
        return service.alterarResponsavelTecnico(id, request.responsavelTecnico());
    }
}
