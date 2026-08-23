package org.fen.fen.repository;

import org.fen.fen.domain.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MedicationRepository extends JpaRepository<Medication, UUID> {
}
