package org.fen.fen.service;

import org.fen.fen.infra.WithSynteticId;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

abstract class BaseCrudServiceTestSupport<T extends WithSynteticId> {

    abstract JpaRepository<T, UUID> repository();

    abstract BaseCrudService<T> service();

    abstract T entity();

    @Test
    void findAllUsesRepositoryPagination() {
        T entity = entity();
        PageRequest pageable = PageRequest.of(0, 10);
        Page<T> page = new PageImpl<>(List.of(entity));
        when(repository().findAll(pageable)).thenReturn(page);

        Page<T> result = service().findAll(pageable);

        assertThat(result).isSameAs(page);
        verify(repository()).findAll(pageable);
    }

    @Test
    void saveUsesRepository() {
        T entity = entity();
        when(repository().save(entity)).thenReturn(entity);

        T result = service().save(entity);

        assertThat(result).isSameAs(entity);
        verify(repository()).save(entity);
    }

    @Test
    void updateLoadsExistingEntityAndSavesIncomingEntityWithId() {
        UUID id = UUID.randomUUID();
        T existing = entity();
        T incoming = entity();
        when(repository().findById(id)).thenReturn(Optional.of(existing));
        when(repository().save(incoming)).thenReturn(incoming);

        T result = service().update(id, incoming);

        assertThat(result).isSameAs(incoming);
        assertThat(incoming.getId()).isEqualTo(id);
        verify(repository()).findById(id);
        verify(repository()).save(incoming);
    }

    @Test
    void deleteByIdUsesRepository() {
        UUID id = UUID.randomUUID();

        service().deleteById(id);

        verify(repository()).deleteById(id);
    }
}
