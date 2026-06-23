package com.ax.template.authblueprint.reconciliation;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
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
 * ReconciliationItem rows are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class ReconciliationService {

    private final ReconciliationRunRepository runs;
    private final MemberWriter members;
    private final ReconciliationMetrics metrics;
    private final Clock clock;

    public ReconciliationService(ReconciliationRunRepository runs, MemberWriter members,
                                 ReconciliationMetrics metrics, Clock clock) {
        this.runs = runs;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * RECON-CLASSIFY/IDEMPOTENT-001 — match the internal set against the external feed snapshot,
     * classifying every distinct key exactly once with its basis. Idempotent on
     * (sourceKey, feedSnapshotHash): a re-run on the SAME feed returns the EXISTING run verbatim
     * (no new run, no duplicate items); a CHANGED feed appends a NEW run, prior retained.
     */
    @Transactional
    public ReconciliationRun run(String sourceKey, String feedSnapshotHash,
                                 Map<String, BigDecimal> internal, Map<String, BigDecimal> external) {
        // RECON-IDEMPOTENT-001 — the same (source, feed-hash) returns the existing run verbatim.
        var existing = runs.findBySourceKeyAndFeedSnapshotHash(sourceKey, feedSnapshotHash);
        if (existing.isPresent()) {
            metrics.record("run", "replayed");
            return existing.get();
        }
        Instant now = Instant.now(clock);
        ReconciliationRun saved;
        try {
            saved = runs.save(new ReconciliationRun(UUID.randomUUID(), sourceKey, feedSnapshotHash, now));
        } catch (DataIntegrityViolationException raced) {
            // a concurrent identical re-run won the uq(source, feed-hash) — return its run.
            metrics.record("run", "replayed");
            return runs.findBySourceKeyAndFeedSnapshotHash(sourceKey, feedSnapshotHash)
                .orElseThrow(ReconciliationException::notFound);
        }
        // the union of keys, deterministically ordered, each classified exactly once.
        TreeSet<String> keys = new TreeSet<>();
        keys.addAll(internal.keySet());
        keys.addAll(external.keySet());
        for (String key : keys) {
            members.persist(new ReconciliationItem(UUID.randomUUID(), saved.getId(), key,
                internal.get(key), external.get(key), now));
        }
        metrics.record("run", "created");
        return saved;
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
