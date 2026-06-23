package com.ax.template.authblueprint.reconciliation;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NO delete method is declared anywhere in this domain — a reconciliation run is resolved, never
 * removed, and its prior runs are retained (RECON-IDEMPOTENT-001). {@link ReconciliationItem}
 * rows are members (HG-AGG-REPO): they own no repository — reads are root-JPQL here, writes go
 * through {@code common/MemberWriter}.
 */
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID> {

    /** RECON-IDEMPOTENT-001 — the (source, feed-hash) identity: a re-run on the SAME feed returns this run. */
    Optional<ReconciliationRun> findBySourceKeyAndFeedSnapshotHash(String sourceKey, String feedSnapshotHash);

    /** RECON-RESOLVE-001 — the run row serializes the read-undisposed-count / write-RESOLVED resolve sequence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReconciliationRun r WHERE r.id = :id")
    Optional<ReconciliationRun> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — ReconciliationItem owns no repository) ──

    @Query("SELECT i FROM ReconciliationItem i WHERE i.runId = :runId ORDER BY i.itemKey ASC")
    List<ReconciliationItem> findItems(@Param("runId") UUID runId);

    /**
     * RECON-CONCURRENT-001 — the item row serializes the read-undisposed / write-disposition
     * dispose sequence so concurrent disposes converge to exactly one winner (CWE-362).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ReconciliationItem i WHERE i.id = :itemId")
    Optional<ReconciliationItem> findItemByIdForUpdate(@Param("itemId") UUID itemId);

    /** RECON-RESOLVE-001 — the resolve gate: a run cannot resolve while any break is undisposed. */
    @Query("SELECT COUNT(i) FROM ReconciliationItem i"
        + " WHERE i.runId = :runId AND i.classification = com.ax.template.authblueprint.reconciliation.ItemClassification.BREAK"
        + " AND i.disposed = FALSE")
    long countUndisposedBreaks(@Param("runId") UUID runId);
}
