package org.fen.fen.funcionario;

import java.util.UUID;
import org.fen.fen.controller.FuncionarioController;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.dto.FuncionarioDetailResponse;
import org.fen.fen.error.ApiExceptionHandler;
import org.fen.fen.service.FuncionarioService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FuncionarioControllerTest {
    @Test void patchesOnlyTechnicalResponsibility() throws Exception {
        FuncionarioService service = mock(FuncionarioService.class); UUID id = UUID.randomUUID();
        when(service.alterarResponsavelTecnico(id, true)).thenReturn(new FuncionarioDetailResponse(id, "Ana", "ana@fen.br", "123", null, Role.FARMACEUTICO, SituacaoUsuario.ATIVO, "PR-1", true, null, null, null, null));
        MockMvcBuilders.standaloneSetup(new FuncionarioController(service)).setControllerAdvice(new ApiExceptionHandler()).build()
                .perform(patch("/api/admin/funcionarios/{id}/responsavel-tecnico", id).contentType("application/json").content("{\"responsavelTecnico\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.responsavelTecnico").value(true));
        verify(service).alterarResponsavelTecnico(id, true);
    }
    @Test void declaresAdminAuthorization() { PreAuthorize auth = FuncionarioController.class.getAnnotation(PreAuthorize.class); assertThat(auth.value()).isEqualTo("hasRole('ADMIN')"); }
}
