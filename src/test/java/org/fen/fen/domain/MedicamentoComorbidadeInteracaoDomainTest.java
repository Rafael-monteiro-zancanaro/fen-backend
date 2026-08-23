package org.fen.fen.domain;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class MedicamentoComorbidadeInteracaoDomainTest {

    @Test
    void entidadesUsamNomesDefinitivosAuditoriaEEntidadeAssociativa() throws Exception {
        assertDomainEntity(Medicamento.class, "medicamento");
        assertDomainEntity(Comorbidade.class, "comorbidade");
        assertDomainEntity(Interacao.class, "interacao");

        assertJoinColumn(Interacao.class.getDeclaredField("comorbidade"), "comorbidadeid");
        assertJoinColumn(Interacao.class.getDeclaredField("medicamento"), "medicamentoid");
    }

    private void assertDomainEntity(Class<?> type, String tableName) {
        assertThat(type.getSuperclass()).isEqualTo(AuditableEntity.class);
        assertThat(type).hasAnnotation(Audited.class);
        assertThat(type.getAnnotation(Table.class).name()).isEqualTo(tableName);
    }

    private void assertJoinColumn(Field field, String columnName) {
        assertThat(field.getAnnotation(JoinColumn.class)).isNotNull();
        assertThat(field.getAnnotation(JoinColumn.class).name()).isEqualTo(columnName);
    }
}
