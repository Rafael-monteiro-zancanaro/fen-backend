package org.fen.fen.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.fen.fen.medicamento.dto.MedicamentoRequest;
import org.fen.fen.medicamento.dto.MedicamentoResponse;
import org.fen.fen.medicamento.dto.MedicamentoSummaryResponse;
import org.fen.fen.service.MedicamentoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/medicamentos")
public class MedicamentoController {
    private final MedicamentoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentoResponse criar(@Valid @RequestBody MedicamentoRequest request) { return service.criar(request); }

    @GetMapping("/{id}")
    public MedicamentoResponse buscar(@PathVariable UUID id) { return service.buscarPorId(id); }

    @GetMapping
    public Page<MedicamentoResponse> listar(@RequestParam(defaultValue = "") String query,
            @PageableDefault(size = 10) Pageable pageable) {
        return service.listar(query, pageable);
    }

    @GetMapping("/autocomplete")
    public List<MedicamentoSummaryResponse> autocomplete(@RequestParam String query,
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit) {
        return service.autocomplete(query, limit);
    }

    @PutMapping("/{id}")
    public MedicamentoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody MedicamentoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
