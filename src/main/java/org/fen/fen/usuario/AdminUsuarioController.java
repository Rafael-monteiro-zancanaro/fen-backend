package org.fen.fen.usuario;

import org.fen.fen.usuario.dto.UsuarioPendenteDetailResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/pendentes")
    public List<UsuarioPendenteSummaryResponse> findPendentes() {
        return usuarioService.findPendentes();
    }

    @GetMapping("/{id}")
    public UsuarioPendenteDetailResponse findPendente(@PathVariable UUID id) {
        return usuarioService.findPendente(id);
    }

    @PostMapping("/{id}/aprovar")
    public void aprovar(@PathVariable UUID id) {
        usuarioService.aprovar(id);
    }

    @DeleteMapping("/{id}/rejeitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejeitar(@PathVariable UUID id) {
        usuarioService.rejeitar(id);
    }
}
