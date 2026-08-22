package org.fen.fen.repository;

import jakarta.persistence.EntityManagerFactory;
import org.fen.fen.usuario.FuncionarioRepository;
import org.fen.fen.usuario.UsuarioMapper;
import org.fen.fen.usuario.UsuarioRepository;
import org.fen.fen.usuario.UsuarioService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@Sql("/datasets/usuario-repository.sql")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:pending-query;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class UsuarioPendingQueryTest extends BaseRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void listsMultiplePendingRegistrationsInOrderWithOneDatabaseQuery() {
        UsuarioService usuarioService = new UsuarioService(
                usuarioRepository,
                funcionarioRepository,
                new BCryptPasswordEncoder(),
                new UsuarioMapper()
        );
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var responses = usuarioService.findPendentes();

        assertThat(responses)
                .extracting(response -> response.email())
                .containsExactly(
                        "primeira.pendente@fen.br",
                        "pendente@fen.br",
                        "ultima.pendente@fen.br"
                );
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
