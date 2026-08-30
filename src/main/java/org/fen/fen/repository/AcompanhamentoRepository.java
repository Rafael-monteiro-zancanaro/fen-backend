package org.fen.fen.repository;

import jakarta.persistence.LockModeType;
import org.fen.fen.domain.Acompanhamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AcompanhamentoRepository extends JpaRepository<Acompanhamento, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Acompanhamento a join fetch a.atendimentoAtual where a.atendimentoAtual.id = :atendimentoId")
    Optional<Acompanhamento> buscarPorAtendimentoAtualComLock(@Param("atendimentoId") UUID atendimentoId);
}
