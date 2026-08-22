package org.fen.fen.service;

import org.fen.fen.infra.WithSynteticId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.NoSuchElementException;
import java.util.UUID;

public abstract class BaseCrudService<T extends WithSynteticId> {

    private final JpaRepository<T, UUID> repository;

    protected BaseCrudService(JpaRepository<T, UUID> repository) {
        this.repository = repository;
    }

    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public T save(T entity) {
        return repository.save(entity);
    }

    public T update(UUID id, T entity) {
        repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Registro não encontrado: " + id));
        entity.setId(id);
        return repository.save(entity);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
