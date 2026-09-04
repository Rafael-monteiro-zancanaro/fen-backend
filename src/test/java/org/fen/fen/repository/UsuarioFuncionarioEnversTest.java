package org.fen.fen.repository;

import org.fen.fen.domain.Funcionario;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.fen.fen.mapper.UsuarioMapper;
import org.fen.fen.service.UsuarioService;
import org.fen.fen.service.FuncionarioService;
import org.fen.fen.mapper.FuncionarioMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:usuario-funcionario-envers;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UsuarioFuncionarioEnversTest extends BaseRepositoryTest {

    private static final AtomicLong REGISTRATION_SEQUENCE = new AtomicLong();

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

    @Test
    void auditsTechnicalResponsibilityWithAuthenticatedAdministrator() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        RegistrationIds ids = transaction.execute(status -> createRegistration());
        transaction.executeWithoutResult(status -> usuarioService().aprovar(ids.usuarioId()));
        transaction.executeWithoutResult(status -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin@fen.br", "N/A",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            );
            funcionarioService().alterarResponsavelTecnico(ids.funcionarioId(), true);
            funcionarioRepository.flush();
        });
        SecurityContextHolder.clearContext();

        assertThat(jdbcTemplate.queryForObject("select responsaveltecnico from aud.funcionario_aud where id = ? order by rev desc limit 1", Boolean.class, ids.funcionarioId())).isTrue();
        assertThat(jdbcTemplate.queryForObject("select updatedby from aud.funcionario_aud where id = ? order by rev desc limit 1", String.class, ids.funcionarioId())).isEqualTo("admin@fen.br");
    }

    private RegistrationIds createRegistration() {
        long sequence = REGISTRATION_SEQUENCE.incrementAndGet();
        Usuario usuario = new Usuario();
        usuario.setEmail("auditada." + sequence + "@fen.br");
        usuario.setPasswordHash("verifier-create");
        usuario.setRole(Role.FARMACEUTICO);
        usuario.setSituacao(SituacaoUsuario.PENDENTE);
        usuarioRepository.saveAndFlush(usuario);

        Funcionario funcionario = new Funcionario();
        funcionario.setUsuario(usuario);
        funcionario.setNome("Funcionária Auditada");
        funcionario.setCpf(String.format("%011d", sequence));
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

    private FuncionarioService funcionarioService() {
        return new FuncionarioService(funcionarioRepository, new FuncionarioMapper());
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
