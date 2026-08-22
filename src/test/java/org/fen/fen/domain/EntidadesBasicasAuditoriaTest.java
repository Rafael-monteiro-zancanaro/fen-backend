package org.fen.fen.domain;

import org.junit.jupiter.api.Test;
import org.fen.fen.infra.AuditableEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class EntidadesBasicasAuditoriaTest {

    @Test
    void exposesTheDocumentedAuditProperties() {
        assertThat(Arrays.stream(AuditableEntity.class.getDeclaredFields())
                .map(Field::getName))
                .containsExactlyInAnyOrder("createdAt", "updatedAt", "createdBy", "updatedBy");

        assertThat(Arrays.stream(AuditableEntity.class.getDeclaredMethods())
                .map(Method::getName))
                .contains("getCreatedAt", "getUpdatedAt", "getCreatedBy", "getUpdatedBy");

        assertThat(Modifier.isAbstract(AuditableEntity.class.getModifiers())).isTrue();
    }
}
