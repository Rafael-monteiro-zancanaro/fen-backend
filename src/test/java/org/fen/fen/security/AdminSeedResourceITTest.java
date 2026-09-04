package org.fen.fen.security;

import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.fen.fen.repository.UsuarioRepository;
import org.fen.fen.service.UsuarioService;
import org.fen.fen.domain.dto.UsuarioRegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:adminseed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AdminSeedResourceITTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_PASSWORD_HASH =
            "$2a$10$qd1i3rFWG/IGuAetEkepKOZphFx4mmPm4MMMAj8VMmyGRI0wehdCq";
    private static final String REGISTRATION_PASSWORD = "registration-secret";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loadsActiveAdministratorWithCompatibleBcryptCredentialsAndSystemAudit() {
        Usuario admin = usuarioRepository.findByEmailIgnoreCase("admin@fen.br").orElseThrow();

        assertThat(admin.getId()).isEqualTo(ADMIN_ID);
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(admin.getPasswordHash()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isNotNull();
        assertThat(admin.getCreatedBy()).isEqualTo("Sistema");
        assertThat(admin.getUpdatedBy()).isEqualTo("Sistema");
    }

    @Test
    void startupLogsDoNotExposeAdministratorCredentials(CapturedOutput output) {
        assertThat(output.getAll())
                .doesNotContain(ADMIN_PASSWORD_HASH)
                .doesNotContain(ADMIN_PASSWORD);
    }

    @Test
    @Transactional
    void registrationLogsDoNotExposeRawOrEncodedPassword(CapturedOutput output) {
        usuarioService.register(new UsuarioRegisterRequest(
                "Usuária de Log",
                "12345678901",
                null,
                "log-registration@fen.br",
                REGISTRATION_PASSWORD,
                Role.FARMACEUTICO,
                "PR-12345",
                null,
                null,
                null,
                null
        ));
        usuarioRepository.flush();
        String storedHash = usuarioRepository.findByEmailIgnoreCase("log-registration@fen.br")
                .orElseThrow()
                .getPasswordHash();

        assertThat(output.getAll())
                .doesNotContain(REGISTRATION_PASSWORD)
                .doesNotContain(storedHash);
    }

}
