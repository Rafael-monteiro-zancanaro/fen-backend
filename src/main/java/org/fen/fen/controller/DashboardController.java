package org.fen.fen.controller;

import lombok.RequiredArgsConstructor;
import org.fen.fen.dashboard.dto.DashboardResponse;
import org.fen.fen.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    @GetMapping
    public DashboardResponse resumo() {
        return service.buscarResumo();
    }
}
