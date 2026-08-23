package org.fen.fen.repository;

import org.fen.fen.domain.Funcionario;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.fen.fen.mapper.UsuarioMapper;
import org.fen.fen.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:usuario-funcionario-envers;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UsuarioFuncionarioEnversTest extends BaseRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void recordsCreateUpdateAndRejectionDeleteRevisionsWithoutCredentialVerifier() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        RegistrationIds ids = transaction.execute(status -> createRegistration());

        transaction.executeWithoutResult(status -> updateRegistration(ids));
        transaction.executeWithoutResult(status -> usuarioService().rejeitar(ids.usuarioId()));

        assertThat(revisionTypes("aud.usuario_aud", ids.usuarioId()))
                .containsExactly(0, 1, 2);
        assertThat(revisionTypes("aud.funcionario_aud", ids.funcionarioId()))
                .containsExactly(0, 1, 2);
        assertThat(auditPasswordColumnCount()).isZero();
    }

    private RegistrationIds createRegistration() {
        Usuario usuario = new Usuario();
        usuario.setEmail("auditada@fen.br");
        usuario.setPasswordHash("verifier-create");
        usuario.setRole(Role.FARMACEUTICO);
        usuario.setSituacao(SituacaoUsuario.PENDENTE);
        usuarioRepository.saveAndFlush(usuario);

        Funcionario funcionario = new Funcionario();
        funcionario.setUsuario(usuario);
        funcionario.setNome("Funcionária Auditada");
        funcionario.setCpf("88888888888");
        funcionario.setCrf("PR-88888");
        funcionario.setResponsavelTecnico(false);
        funcionarioRepository.saveAndFlush(funcionario);

        return new RegistrationIds(usuario.getId(), funcionario.getId());
    }

    private void updateRegistration(RegistrationIds ids) {
        Usuario usuario = usuarioRepository.findById(ids.usuarioId()).orElseThrow();
        usuario.setEmail("auditada.atualizada@fen.br");
        usuario.setPasswordHash("verifier-update");
        usuarioRepository.saveAndFlush(usuario);

        Funcionario funcionario = funcionarioRepository.findById(ids.funcionarioId()).orElseThrow();
        funcionario.setNome("Funcionária Auditada Atualizada");
        funcionarioRepository.saveAndFlush(funcionario);
    }

    private UsuarioService usuarioService() {
        return new UsuarioService(
                usuarioRepository,
                funcionarioRepository,
                new BCryptPasswordEncoder(),
                new UsuarioMapper()
        );
    }

    private List<Integer> revisionTypes(String auditTable, UUID id) {
        return jdbcTemplate.queryForList(
                "select revtype from " + auditTable + " where id = ? order by rev",
                Integer.class,
                id
        );
    }

    private int auditPasswordColumnCount() {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where lower(table_schema) = 'aud'
                  and lower(table_name) = 'usuario_aud'
                  and lower(column_name) = 'passwordhash'
                """, Integer.class);
    }

    private record RegistrationIds(UUID usuarioId, UUID funcionarioId) {
    }
}
