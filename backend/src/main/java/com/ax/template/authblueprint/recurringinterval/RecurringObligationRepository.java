package com.ax.template.authblueprint.recurringinterval;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RecurringObligationRepository extends JpaRepository<RecurringObligation, UUID> {

    Optional<RecurringObligation> findByObligationKey(String obligationKey);

    boolean existsByObligationKey(String obligationKey);

    /** CRI-CONCURRENT-001 — the obligation row is the single serialization point for a
     *  completion (window advance) and the sweep. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM RecurringObligation o WHERE o.obligationKey = :obligationKey")
    Optional<RecurringObligation> findByObligationKeyForUpdate(@Param("obligationKey") String obligationKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM RecurringObligation o WHERE o.id = :id")
    Optional<RecurringObligation> findByIdForUpdate(@Param("id") UUID id);

    /** Sweep worklist — OPEN obligations, oldest first, bounded. */
    Page<RecurringObligation> findByStatusOrderByCreatedAtAsc(RecurringObligationStatus status, Pageable pageable);

    // ── through-root member reads (HG-AGG-REPO — Occurrence owns no repository) ──

    @Query("SELECT o FROM Occurrence o WHERE o.obligationId = :obligationId ORDER BY o.completedAt ASC")
    Page<Occurrence> findOccurrencesPage(@Param("obligationId") UUID obligationId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Occurrence o WHERE o.obligationId = :obligationId")
    long countOccurrences(@Param("obligationId") UUID obligationId);
}
