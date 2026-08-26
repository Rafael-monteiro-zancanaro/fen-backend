package org.fen.fen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fen.fen.comorbidade.dto.ComorbidadeRequest;
import org.fen.fen.comorbidade.dto.ComorbidadeResponse;
import org.fen.fen.comorbidade.dto.ComorbidadeSummaryResponse;
import org.fen.fen.service.ComorbidadeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comorbidades")
public class ComorbidadeController {
    private final ComorbidadeService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComorbidadeResponse criar(@Valid @RequestBody ComorbidadeRequest request) { return service.criar(request); }

    @GetMapping("/{id}")
    public ComorbidadeResponse buscar(@PathVariable UUID id) { return service.buscarPorId(id); }

    @GetMapping
    public Page<ComorbidadeSummaryResponse> listar(@RequestParam(defaultValue = "") String query,
            @PageableDefault(size = 10) Pageable pageable) {
        return service.listar(query, pageable);
    }

    @PutMapping("/{id}")
    public ComorbidadeResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ComorbidadeRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
