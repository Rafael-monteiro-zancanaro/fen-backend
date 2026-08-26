package org.fen.fen.domain.dto;

import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.TipoEstagio;
import java.time.LocalDate;
import java.util.UUID;

public record FuncionarioDetailResponse(UUID id, String nome, String email, String cpf,
        LocalDate dataNascimento, Role role, SituacaoUsuario situacao, String crf,
        Boolean responsavelTecnico, TipoEstagio tipoEstagio, SupervisorResponse supervisor,
        LocalDate inicioVigencia, LocalDate fimVigencia) { }
