package org.fen.fen.security;

import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.fen.fen.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FenUserDetailsServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");

    @Test
    void loadsActiveAccountWithEmailAndRoleAuthority() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        when(repository.findByEmailIgnoreCase("farmaceutico@fen.br"))
                .thenReturn(Optional.of(usuario(SituacaoUsuario.ATIVO)));
        FenUserDetailsService service = new FenUserDetailsService(repository);

        UserDetails result = service.loadUserByUsername("farmaceutico@fen.br");

        assertThat(result.getUsername()).isEqualTo("farmaceutico@fen.br");
        assertThat(result.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_FARMACEUTICO");
        assertThat(result).isInstanceOfSatisfying(FenUserDetails.class,
                details -> assertThat(details.userId()).isEqualTo(USUARIO_ID));
    }

    @Test
    void loadsPendingAccountAsDisabled() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        when(repository.findByEmailIgnoreCase("farmaceutico@fen.br"))
                .thenReturn(Optional.of(usuario(SituacaoUsuario.PENDENTE)));
        FenUserDetailsService service = new FenUserDetailsService(repository);

        UserDetails result = service.loadUserByUsername("farmaceutico@fen.br");

        assertThat(result.isEnabled()).isFalse();
    }

    private Usuario usuario(SituacaoUsuario situacao) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setEmail("farmaceutico@fen.br");
        usuario.setPasswordHash("$2a$10$hash");
        usuario.setRole(Role.FARMACEUTICO);
        usuario.setSituacao(situacao);
        return usuario;
    }
}
