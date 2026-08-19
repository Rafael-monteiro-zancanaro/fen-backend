package org.fen.fen.dto.request;

import java.time.LocalDate;

public record RegisterRequestDTO(
        String nome,
        LocalDate dataNascimento,
        String hashSenha

) {
}
