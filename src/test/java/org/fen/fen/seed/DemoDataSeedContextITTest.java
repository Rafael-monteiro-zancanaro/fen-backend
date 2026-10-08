package org.fen.fen.seed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:production-demo-seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.liquibase.contexts=production",
        "spring.liquibase.drop-first=true"
})
@ActiveProfiles("test")
class DemoDataSeedContextITTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsConsistentDemoDataWhenProductionContextIsEnabled() {
        assertCount("medicamento", 35);
        assertCount("comorbidade", 35);
        assertCount("interacao", 48);
        assertCount("paciente", 15);
        assertCount("pacientecomorbidade", 28);

        assertQueryCount("select count(distinct medicamentoid) from interacao", 31);
        assertQueryCount("select count(distinct comorbidadeid) from interacao", 23);
        assertQueryCount("select count(distinct pacienteid) from pacientecomorbidade", 12);
        assertQueryCount("""
                select count(*) from (
                    select comorbidadeid, medicamentoid
                    from interacao
                    group by comorbidadeid, medicamentoid
                ) pares
                """, 48);
    }

    private void assertCount(String tableName, int expected) {
        assertQueryCount("select count(*) from " + tableName, expected);
    }

    private void assertQueryCount(String sql, int expected) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count).isEqualTo(expected);
    }
}
