package org.fen.fen.controller;

import jakarta.validation.Valid;
import org.fen.fen.service.UsuarioService;
import org.fen.fen.domain.dto.SupervisorResponse;
import org.fen.fen.domain.dto.UsuarioRegisterRequest;
import org.fen.fen.domain.dto.UsuarioRegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRegisterResponse register(@Valid @RequestBody UsuarioRegisterRequest request) {
        return usuarioService.register(request);
    }

    @GetMapping("/api/public/supervisores")
    public List<SupervisorResponse> findSupervisoresAtivos() {
        return usuarioService.findSupervisoresAtivos();
    }
}
