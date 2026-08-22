package org.fen.fen.security;

import org.fen.fen.usuario.Role;
import org.fen.fen.usuario.SituacaoUsuario;
import org.fen.fen.usuario.Usuario;
import org.fen.fen.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:adminseed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
)
@ActiveProfiles("test")
class AdminSeedResourceITTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loadsActiveAdministratorWithCompatibleBcryptCredentialsAndSystemAudit() {
        Usuario admin = usuarioRepository.findByEmailIgnoreCase("admin@fen.br").orElseThrow();

        assertThat(admin.getId()).isEqualTo(ADMIN_ID);
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(admin.getPasswordHash()).isNotEqualTo("admin123");
        assertThat(passwordEncoder.matches("admin123", admin.getPasswordHash())).isTrue();
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isNotNull();
        assertThat(admin.getCreatedBy()).isEqualTo("Sistema");
        assertThat(admin.getUpdatedBy()).isEqualTo("Sistema");
    }

}
