package com.ax.template.authblueprint.reconciliation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * external-reconciliation-l0 sole orchestrator. A run ingests an internal record set and an
 * external feed snapshot and classifies every distinct key EXACTLY ONCE into MATCHED / BREAK /
 * INTERNAL_ONLY / EXTERNAL_ONLY, recording each item's basis (internal/external amount + delta)
 * — a bare aggregate count is unrepresentable (RECON-CLASSIFY-001). A BREAK requires explicit
 * human disposition (RECON-DISPOSE-001); a run cannot be RESOLVED while any break is undisposed
 * (RECON-RESOLVE-001). The run is idempotent on (sourceKey, feedSnapshotHash): a re-run on the
 * SAME feed returns the EXISTING run, a CHANGED feed appends a new run, prior retained
 * (RECON-IDEMPOTENT-001). The dispose path takes the item's PESSIMISTIC_WRITE row lock so
 * concurrent disposes converge to exactly one winner (RECON-CONCURRENT-001 / CWE-362).
 * ReconciliationItem rows are members: {@link ReconciliationRunCreator} writes them through the
 * MemberWriter seam inside the run's atomic transaction, root-JPQL reads.
 */
@Service
public class ReconciliationService {

    private final ReconciliationRunRepository runs;
    private final ReconciliationRunCreator creator;
    private final ReconciliationMetrics metrics;
    private final Clock clock;

    public ReconciliationService(ReconciliationRunRepository runs, ReconciliationRunCreator creator,
                                 ReconciliationMetrics metrics, Clock clock) {
        this.runs = runs;
        this.creator = creator;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * RECON-CLASSIFY/IDEMPOTENT-001 — match the internal set against the external feed snapshot,
     * classifying every distinct key exactly once with its basis. Idempotent on
     * (sourceKey, feedSnapshotHash): a re-run on the SAME feed returns the EXISTING run verbatim
     * (no new run, no duplicate items); a CHANGED feed appends a NEW run, prior retained.
     */
    // NOT @Transactional (audit-seal-11 P1-65 revert) — the DataIntegrityViolationException catch
    // MUST sit OUTSIDE the run+items transaction. run() owns no tx of its own; the atomic unit is
    // ReconciliationRunCreator.doRun, and the loser of a race rolls THAT whole unit back before this
    // catch requeries the winner in a fresh tx.
    public ReconciliationRun run(String sourceKey, String feedSnapshotHash,
                                 Map<String, BigDecimal> internal, Map<String, BigDecimal> external) {
        // RECON-IDEMPOTENT-001 — sequential-retry fast path: the same (source, feed-hash) returns the
        // existing run verbatim (no new run, no duplicate items). Keeps the common idempotent case
        // off the constraint-violation path entirely.
        var existing = runs.findBySourceKeyAndFeedSnapshotHash(sourceKey, feedSnapshotHash);
        if (existing.isPresent()) {
            metrics.record("run", "replayed");
            return existing.get();
        }
        try {
            // creator.doRun inserts the run AND persists all classified items in ONE atomic tx: the
            // run insert is NON-TERMINAL, so it must NOT be isolated in a REQUIRES_NEW inner tx — an
            // independently-committed run that then lost its items would be a permanent orphan that a
            // later re-run short-circuits onto. Because run() holds no tx, a concurrent identical run
            // that wins the uq(source, feed-hash) makes doRun's saveAndFlush throw; doRun's tx
            // (run + items) rolls back ATOMICALLY, the exception surfaces HERE, outside any tx, and we
            // requery the winner in a fresh tx — no orphan, no poisoned-tx (25P02) 500.
            ReconciliationRun saved = creator.doRun(sourceKey, feedSnapshotHash, internal, external);
            metrics.record("run", "created");
            return saved;
        } catch (DataIntegrityViolationException raced) {
            metrics.record("run", "replayed");
            return creator.replay(sourceKey, feedSnapshotHash);
        }
    }

    /**
     * RECON-DISPOSE/CONCURRENT-001 — record the human disposition of a BREAK exactly once. The
     * item row's PESSIMISTIC_WRITE lock serializes the read-undisposed / write-disposition
     * sequence so across N concurrent disposes exactly one wins; the rest find it already
     * disposed → 409 (CWE-362). Only a BREAK can be disposed; the reason must be non-blank.
     */
    @Transactional
    public ReconciliationItem dispose(UUID runId, UUID itemId, DispositionType type, String reason, String actor) {
        // RECON-CONCURRENT-001 — load the item under its PESSIMISTIC_WRITE row lock.
        ReconciliationItem item = runs.findItemByIdForUpdate(itemId).orElseThrow(ReconciliationException::notFound);
        if (!item.getRunId().equals(runId)) {
            throw ReconciliationException.notFound();           // IDOR-safe — item not under this run
        }
        if (!item.isBreak()) {
            metrics.record("dispose", "not_a_break");
            throw ReconciliationException.notABreak();
        }
        if (reason == null || reason.isBlank()) {
            metrics.record("dispose", "blank_reason");
            throw ReconciliationException.blankReason();
        }
        if (item.isDisposed()) {
            metrics.record("dispose", "already_disposed");      // loser of the concurrent dispose
            throw ReconciliationException.alreadyDisposed();
        }
        item.dispose(type, actor, Instant.now(clock), reason);
        metrics.record("dispose", "disposed");
        return item;
    }

    /**
     * RECON-RESOLVE-001 — mark the run RESOLVED only when every break is disposed. An undisposed
     * break is refused (422). Resolving an already-RESOLVED run is idempotent. The run row lock
     * serializes the read-undisposed-count / write-RESOLVED sequence.
     */
    @Transactional
    public ReconciliationRun resolve(UUID runId) {
        ReconciliationRun run = runs.findByIdForUpdate(runId).orElseThrow(ReconciliationException::notFound);
        if (run.isResolved()) {
            metrics.record("resolve", "resolved");              // idempotent no-op
            return run;
        }
        if (runs.countUndisposedBreaks(runId) > 0) {
            metrics.record("resolve", "undisposed_break");
            throw ReconciliationException.undisposedBreak();
        }
        run.resolve(Instant.now(clock));
        metrics.record("resolve", "resolved");
        return run;
    }

    @Transactional(readOnly = true)
    public ReconciliationRun get(UUID runId) {
        return runs.findById(runId).orElseThrow(ReconciliationException::notFound);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationItem> items(UUID runId) {
        get(runId);                                             // 404 before an empty list
        return runs.findItems(runId);
    }

    /** Build a deterministic (key → amount) map from request entries; a duplicate key keeps the last. */
    static Map<String, BigDecimal> toMap(List<? extends FeedEntry> entries) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (entries != null) {
            for (FeedEntry e : entries) {
                map.put(e.key(), e.amount());
            }
        }
        return map;
    }

    /** A single (key, amount) line of an internal record set or external feed snapshot. */
    public interface FeedEntry {
        String key();
        BigDecimal amount();
    }
}
