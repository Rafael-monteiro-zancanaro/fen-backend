package org.fen.fen.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.fen.fen.validation.Utf8ByteLength;

public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72) @Utf8ByteLength(max = 72) String senha
) {
    public LoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
