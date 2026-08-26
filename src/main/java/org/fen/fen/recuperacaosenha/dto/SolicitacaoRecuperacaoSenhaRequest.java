package org.fen.fen.recuperacaosenha.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.fen.fen.validation.Utf8ByteLength;

public record SolicitacaoRecuperacaoSenhaRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) @Utf8ByteLength(max = 72) String novaSenha
) {
    public SolicitacaoRecuperacaoSenhaRequest {
        if (email != null) email = email.trim();
    }
}
