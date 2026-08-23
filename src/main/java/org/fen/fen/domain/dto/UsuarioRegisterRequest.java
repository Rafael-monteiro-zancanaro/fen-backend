package org.fen.fen.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.TipoEstagio;
import org.fen.fen.validation.Utf8ByteLength;

import java.time.LocalDate;
import java.util.UUID;

public record UsuarioRegisterRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank
        @Pattern(regexp = "(?:\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})")
        String cpf,
        LocalDate dataNascimento,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) @Utf8ByteLength(max = 72) String senha,
        @NotNull Role role,
        @Size(max = 20) String crf,
        Boolean responsavelTecnico,
        TipoEstagio tipoEstagio,
        UUID supervisorId,
        LocalDate inicioVigencia,
        LocalDate fimVigencia
) {
    public UsuarioRegisterRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
