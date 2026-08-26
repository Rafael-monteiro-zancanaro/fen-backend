package org.fen.fen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fen.fen.domain.StatusSolicitacaoRecuperacaoSenha;
import org.fen.fen.recuperacaosenha.dto.SolicitacaoRecuperacaoSenhaRequest;
import org.fen.fen.recuperacaosenha.dto.SolicitacaoRecuperacaoSenhaResponse;
import org.fen.fen.service.SolicitacaoRecuperacaoSenhaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recuperacoes-senha")
public class SolicitacaoRecuperacaoSenhaController {
    private final SolicitacaoRecuperacaoSenhaService service;
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SolicitacaoRecuperacaoSenhaResponse criar(@Valid @RequestBody SolicitacaoRecuperacaoSenhaRequest request) { return service.criar(request); }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public Page<SolicitacaoRecuperacaoSenhaResponse> listar(@RequestParam(defaultValue = "") String email,
            @RequestParam(required = false) StatusSolicitacaoRecuperacaoSenha status,
            @PageableDefault(size = 10) Pageable pageable) { return service.listar(email, status, pageable); }
    @GetMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public SolicitacaoRecuperacaoSenhaResponse buscar(@PathVariable UUID id) { return service.buscar(id); }
    @PostMapping("/{id}/aprovar") @PreAuthorize("hasRole('ADMIN')")
    public SolicitacaoRecuperacaoSenhaResponse aprovar(@PathVariable UUID id) { return service.aprovar(id); }
    @PostMapping("/{id}/rejeitar") @PreAuthorize("hasRole('ADMIN')")
    public SolicitacaoRecuperacaoSenhaResponse rejeitar(@PathVariable UUID id) { return service.rejeitar(id); }
}
