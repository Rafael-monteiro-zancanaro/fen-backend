package org.fen.fen.domain.dto;

import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import java.util.UUID;

public record FuncionarioSummaryResponse(UUID id, UUID usuarioId, String nome, String email, Role role,
                                         SituacaoUsuario situacao, Boolean responsavelTecnico) { }
