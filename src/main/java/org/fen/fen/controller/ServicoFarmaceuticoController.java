package org.fen.fen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fen.fen.service.ServicoFarmaceuticoService;
import org.fen.fen.service.AnexoAtendimentoService;
import org.fen.fen.servicofarmaceutico.dto.AnexoAtendimentoResponse;
import org.fen.fen.servicofarmaceutico.dto.ContinuacaoResponse;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoBuscaAvancadaResponse;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoRequest;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoResponse;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoResumoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/servicos-farmaceuticos")
public class ServicoFarmaceuticoController {
    private final ServicoFarmaceuticoService service;
    private final AnexoAtendimentoService anexoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoFarmaceuticoResponse criar(@Valid @RequestBody ServicoFarmaceuticoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public ServicoFarmaceuticoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoFarmaceuticoRequest request
    ) {
        return service.atualizar(id, request);
    }

    @GetMapping
    public Page<ServicoFarmaceuticoResumoResponse> listar(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "false") boolean retornoHoje,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return service.listar(query, status, retornoHoje, pageable);
    }

    @GetMapping("/busca-avancada")
    public Page<ServicoFarmaceuticoBuscaAvancadaResponse> buscarAvancado(
            @RequestParam(defaultValue = "") String cpf,
            @RequestParam(required = false) UUID medicamentoId,
            @RequestParam(defaultValue = "") String lote,
            @RequestParam(required = false) LocalDate dataAtendimento,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return service.buscarAvancado(cpf, medicamentoId, lote, dataAtendimento, pageable);
    }

    @GetMapping("/{id}")
    public ServicoFarmaceuticoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @GetMapping("/{id}/continuacao")
    public ContinuacaoResponse continuacao(@PathVariable UUID id) {
        return service.buscarContinuacao(id);
    }

    @PostMapping("/{id}/retornos")
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoFarmaceuticoResponse criarRetorno(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoFarmaceuticoRequest request
    ) {
        return service.criarRetorno(id, request);
    }

    @PostMapping("/{id}/encerrar")
    public ServicoFarmaceuticoResponse encerrar(@PathVariable UUID id) {
        return service.encerrar(id);
    }

    @PostMapping(path = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoAtendimentoResponse anexar(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        return anexoService.anexar(id, file);
    }

    @GetMapping("/{id}/anexos")
    public java.util.List<AnexoAtendimentoResponse> listarAnexos(@PathVariable UUID id) {
        return anexoService.listar(id);
    }

    @GetMapping("/{id}/anexos/{anexoId}")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable UUID id, @PathVariable UUID anexoId) {
        return anexoService.baixar(id, anexoId);
    }

    @DeleteMapping("/{id}/anexos/{anexoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerAnexo(@PathVariable UUID id, @PathVariable UUID anexoId) {
        anexoService.remover(id, anexoId);
    }
}
