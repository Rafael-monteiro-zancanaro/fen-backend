package org.fen.fen.usuario.dto;

import org.fen.fen.usuario.Role;
import org.fen.fen.usuario.SituacaoUsuario;
import org.fen.fen.usuario.TipoEstagio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioPendenteDetailResponse(
        UUID id,
        UUID funcionarioId,
        String nome,
        String email,
        String cpf,
        LocalDate dataNascimento,
        Role role,
        SituacaoUsuario situacao,
        LocalDateTime solicitadoEm,
        String crf,
        Boolean responsavelTecnico,
        TipoEstagio tipoEstagio,
        SupervisorResponse supervisor,
        LocalDate inicioVigencia,
        LocalDate fimVigencia
) {
}
