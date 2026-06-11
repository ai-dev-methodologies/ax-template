package com.ax.template.authblueprint.obligation;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObligationRepository extends JpaRepository<Obligation, UUID> {

    Optional<Obligation> findByObligationKey(String obligationKey);

    boolean existsByObligationKey(String obligationKey);

    /** OBL-CONCURRENT-001 — the obligation row is the single serialization point
     *  for the sweep, an acknowledgment, and a usage advance. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Obligation o WHERE o.obligationKey = :obligationKey")
    Optional<Obligation> findByObligationKeyForUpdate(@Param("obligationKey") String obligationKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Obligation o WHERE o.id = :id")
    Optional<Obligation> findByIdForUpdate(@Param("id") UUID id);

    /** Sweep worklist — OPEN obligations, oldest first, bounded. */
    Page<Obligation> findByStatusOrderByCreatedAtAsc(ObligationStatus status, Pageable pageable);

    // ── through-root member reads (HG-AGG-REPO — members own no repository) ──

    @Query("SELECT a FROM ObligationAxis a WHERE a.obligationId = :obligationId")
    List<ObligationAxis> findAxes(@Param("obligationId") UUID obligationId);

    @Query("SELECT a FROM ObligationAxis a WHERE a.obligationId = :obligationId AND a.kind = :kind")
    Optional<ObligationAxis> findAxis(@Param("obligationId") UUID obligationId, @Param("kind") AxisKind kind);

    @Query("SELECT COUNT(e) > 0 FROM EscalationEvent e WHERE e.obligationId = :obligationId AND e.rung = :rung")
    boolean rungFired(@Param("obligationId") UUID obligationId, @Param("rung") EscalationRung rung);

    @Query("SELECT e FROM EscalationEvent e WHERE e.obligationId = :obligationId ORDER BY e.firedAt ASC")
    List<EscalationEvent> findEscalations(@Param("obligationId") UUID obligationId);

    @Query("SELECT d FROM DerivationRecord d WHERE d.obligationId = :obligationId ORDER BY d.derivedAt ASC")
    Page<DerivationRecord> findDerivationsPage(@Param("obligationId") UUID obligationId, Pageable pageable);
}
