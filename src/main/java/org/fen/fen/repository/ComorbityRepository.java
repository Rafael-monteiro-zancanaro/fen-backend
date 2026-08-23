package org.fen.fen.repository;

import org.fen.fen.domain.Comorbity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComorbityRepository extends JpaRepository<Comorbity, UUID> {
}
