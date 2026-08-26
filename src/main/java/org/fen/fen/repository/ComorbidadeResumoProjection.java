package org.fen.fen.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ComorbidadeResumoProjection {
    UUID getId();
    String getNome();
    long getInteractionCount();
    LocalDateTime getCreatedAt();
}
