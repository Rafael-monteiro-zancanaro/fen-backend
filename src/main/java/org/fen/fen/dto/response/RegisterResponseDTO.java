package org.fen.fen.dto.response;

public record RegisterResponseDTO(
        String nome,
        String email
) {

    public static RegisterResponseDTO from(Usuario usuario) {
        return new RegisterResponseDTO(
                usuario.getNome(),
                usuario.getEmail()
        );
    }
}
