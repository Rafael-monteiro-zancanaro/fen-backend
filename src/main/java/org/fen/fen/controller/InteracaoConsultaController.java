package org.fen.fen.controller;

import lombok.RequiredArgsConstructor;
import org.fen.fen.interacao.dto.InteracaoResponse;
import org.fen.fen.service.InteracaoConsultaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interacoes")
public class InteracaoConsultaController {
    private final InteracaoConsultaService service;

    @GetMapping
    public List<InteracaoResponse> buscar(@RequestParam Set<UUID> medicamentoIds,
            @RequestParam Set<UUID> comorbidadeIds) {
        return service.buscar(medicamentoIds, comorbidadeIds);
    }
}
