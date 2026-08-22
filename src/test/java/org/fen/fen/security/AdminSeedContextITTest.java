package org.fen.fen.security;

import org.fen.fen.FenApplication;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSeedContextITTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("liquibaseContexts")
    void seedsFixedAdministratorOnlyInExplicitDevelopmentAndTestContexts(
            String profile,
            boolean expectedSeed,
            String expectedLiquibaseContext
    ) {
        List<String> arguments = new ArrayList<>(List.of(
                "--spring.datasource.url=jdbc:h2:mem:seed-context-" + profile
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "--spring.jpa.hibernate.ddl-auto=none",
                "--spring.liquibase.drop-first=true",
                "--fen.security.jwt.secret=0123456789abcdef0123456789abcdef",
                "--server.port=0",
                "--spring.main.banner-mode=off",
                "--logging.level.root=ERROR"
        ));
        if (!"default".equals(profile)) {
            arguments.add("--spring.profiles.active=" + profile);
        }

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FenApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(arguments.toArray(String[]::new))) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            Integer adminCount = jdbcTemplate.queryForObject(
                    "select count(*) from usuario where email = 'admin@fen.br'",
                    Integer.class
            );

            assertThat(adminCount).isEqualTo(expectedSeed ? 1 : 0);
            assertThat(context.getEnvironment().getProperty("spring.liquibase.contexts"))
                    .isEqualTo(expectedLiquibaseContext);
        }
    }

    private static Stream<Arguments> liquibaseContexts() {
        return Stream.of(
                Arguments.of(Named.of("default/production", "default"), false, "production"),
                Arguments.of(Named.of("development", "dev"), true, "dev"),
                Arguments.of(Named.of("test", "test"), true, "test")
        );
    }
}
