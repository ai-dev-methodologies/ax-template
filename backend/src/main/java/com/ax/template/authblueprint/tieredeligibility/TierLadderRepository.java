package com.ax.template.authblueprint.tieredeligibility;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TierLadderRepository extends JpaRepository<TierLadder, UUID> {

    Optional<TierLadder> findByLadderKey(String ladderKey);

    boolean existsByLadderKey(String ladderKey);

    /** TIER-DERIVE-001 / TIER-LADDER-001 — the ladder row is the single serialization point for ALL THREE
     *  write-paths (accrue, restore, use): a use racing a crossing accrual always observes the POST-crossing
     *  tier (CWE-362). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM TierLadder l WHERE l.ladderKey = :ladderKey")
    Optional<TierLadder> findByLadderKeyForUpdate(@Param("ladderKey") String ladderKey);

    // ── through-root member reads (HG-AGG-REPO — TierAccrual / TierRestoreEvent own no repository) ──

    @Query("SELECT COALESCE(MAX(a.sequenceNo), 0) FROM TierAccrual a WHERE a.ladderId = :ladderId")
    long maxAccrualSequence(@Param("ladderId") UUID ladderId);

    @Query("SELECT COALESCE(MAX(r.sequenceNo), 0) FROM TierRestoreEvent r WHERE r.ladderId = :ladderId")
    long maxRestoreSequence(@Param("ladderId") UUID ladderId);

    @Query("SELECT a FROM TierAccrual a WHERE a.ladderId = :ladderId ORDER BY a.sequenceNo ASC")
    Page<TierAccrual> findAccrualsPage(@Param("ladderId") UUID ladderId, Pageable pageable);

    @Query("SELECT r FROM TierRestoreEvent r WHERE r.ladderId = :ladderId ORDER BY r.sequenceNo ASC")
    Page<TierRestoreEvent> findRestoresPage(@Param("ladderId") UUID ladderId, Pageable pageable);
}
