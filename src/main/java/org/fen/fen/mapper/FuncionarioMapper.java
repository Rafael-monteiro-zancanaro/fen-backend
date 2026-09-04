package org.fen.fen.mapper;

import org.fen.fen.domain.Funcionario;
import org.fen.fen.domain.dto.*;
import org.springframework.stereotype.Component;

@Component
public class FuncionarioMapper {
    public FuncionarioSummaryResponse toSummary(Funcionario f) {
        return new FuncionarioSummaryResponse(f.getId(), f.getUsuario().getId(), f.getNome(), f.getUsuario().getEmail(),
                f.getUsuario().getRole(), f.getUsuario().getSituacao(), f.getResponsavelTecnico());
    }
    public FuncionarioDetailResponse toDetail(Funcionario f) {
        SupervisorResponse supervisor = f.getSupervisor() == null ? null :
                new SupervisorResponse(f.getSupervisor().getId(), f.getSupervisor().getNome());
        return new FuncionarioDetailResponse(f.getId(), f.getNome(), f.getUsuario().getEmail(), f.getCpf(),
                f.getDataNascimento(), f.getUsuario().getRole(), f.getUsuario().getSituacao(), f.getCrf(),
                f.getResponsavelTecnico(), f.getTipoEstagio(), supervisor, f.getInicioVigencia(), f.getFimVigencia());
    }
}
