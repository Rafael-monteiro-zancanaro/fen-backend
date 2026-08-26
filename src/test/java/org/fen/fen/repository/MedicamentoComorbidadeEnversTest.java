package org.fen.fen.repository;

import jakarta.persistence.EntityManager;
import org.fen.fen.domain.Comorbidade;
import org.fen.fen.domain.Interacao;
import org.fen.fen.domain.Medicamento;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:cadastrosenvers;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class MedicamentoComorbidadeEnversTest {
    @Autowired TransactionTemplate transactions;
    @Autowired EntityManager entityManager;
    @Autowired MedicamentoRepository medicamentos;
    @Autowired ComorbidadeRepository comorbidades;
    @Autowired InteracaoRepository interacoes;

    @Test
    void registraRevisoesDasTresEntidades() {
        UUID[] ids = transactions.execute(status -> {
            Medicamento medicamento = medicamentos.save(new Medicamento("Dipirona", "mg", "Oral"));
            Comorbidade comorbidade = comorbidades.save(new Comorbidade("Hipertensão"));
            Interacao interacao = interacoes.save(new Interacao(comorbidade, medicamento));
            interacoes.flush();
            return new UUID[] { medicamento.getId(), comorbidade.getId(), interacao.getId() };
        });

        transactions.executeWithoutResult(status -> {
            var reader = AuditReaderFactory.get(entityManager);
            assertThat(reader.getRevisions(Medicamento.class, ids[0])).hasSize(1);
            assertThat(reader.getRevisions(Comorbidade.class, ids[1])).hasSize(1);
            assertThat(reader.getRevisions(Interacao.class, ids[2])).hasSize(1);
        });
    }
}
