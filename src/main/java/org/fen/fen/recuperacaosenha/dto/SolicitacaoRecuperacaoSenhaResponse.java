package org.fen.fen.recuperacaosenha.dto;

import org.fen.fen.domain.StatusSolicitacaoRecuperacaoSenha;
import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoRecuperacaoSenhaResponse(UUID id, String email,
        StatusSolicitacaoRecuperacaoSenha status, LocalDateTime createdAt) {}
