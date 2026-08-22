package org.fen.fen.repository;

import org.fen.fen.usuario.Role;
import org.fen.fen.usuario.SituacaoUsuario;
import org.fen.fen.usuario.Usuario;
import org.fen.fen.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql("/datasets/usuario-repository.sql")
class UsuarioRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void generatesUuidWhenSavingUsuario() {
        Usuario usuario = usuario("nova@fen.br", "12345678901");

        Usuario saved = usuarioRepository.saveAndFlush(usuario);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findsUsuarioByEmailIgnoringCase() {
        assertThat(usuarioRepository.findByEmailIgnoreCase("FARMA@FEN.BR"))
                .get()
                .extracting(Usuario::getRole)
                .isEqualTo(Role.FARMACEUTICO);
        assertThat(usuarioRepository.existsByEmailIgnoreCase("FARMA@FEN.BR")).isTrue();
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        Usuario duplicate = usuario("  FARMA@FEN.BR  ", "12345678901");

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listsUsuariosBySituacaoInCreationOrder() {
        assertThat(usuarioRepository.findAllBySituacaoOrderByCreatedAtAsc(SituacaoUsuario.PENDENTE))
                .extracting(Usuario::getEmail)
                .containsExactly(
                        "primeira.pendente@fen.br",
                        "pendente@fen.br",
                        "ultima.pendente@fen.br"
                );
    }

    private Usuario usuario(String email, String passwordHash) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordHash);
        usuario.setRole(Role.FARMACEUTICO);
        usuario.setSituacao(SituacaoUsuario.PENDENTE);
        return usuario;
    }
}
