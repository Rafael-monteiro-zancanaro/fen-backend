package org.fen.fen.infra;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditorAwareImplTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedUsersName() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "farmaceutico@uem.br",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_FARMACEUTICO"))
                )
        );

        assertThat(new AuditorAwareImpl().getCurrentAuditor()).contains("farmaceutico@uem.br");
    }

    @Test
    void returnsSistemaWithoutAuthentication() {
        assertThat(new AuditorAwareImpl().getCurrentAuditor()).contains("Sistema");
    }

    @Test
    void returnsSistemaForUnauthenticatedAuthentication() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("pendente@uem.br", "password");
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(new AuditorAwareImpl().getCurrentAuditor()).contains("Sistema");
    }

    @Test
    void returnsSistemaForAnonymousAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                )
        );

        assertThat(new AuditorAwareImpl().getCurrentAuditor()).contains("Sistema");
    }

    @Nested
    @SpringBootTest(
            properties = {
                    "spring.datasource.url=jdbc:h2:mem:auditing;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.liquibase.enabled=false",
                    "spring.jpa.hibernate.ddl-auto=none"
            }
    )
    class AuditorRegistrationTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void registersTheConfiguredAuditorAwareBean() {
            assertThat(applicationContext.containsBean("auditorAware")).isTrue();
        }
    }
}
