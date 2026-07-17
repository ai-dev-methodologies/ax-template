package com.ax.template.authblueprint.reconciliation;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Creates a reconciliation run TOGETHER WITH all of its classified items as ONE atomic transaction
 * (RECON-IDEMPOTENT-001). Split into its own bean so {@link ReconciliationService#run} can catch a
 * {@code uq(source_key, feed_snapshot_hash)} {@link org.springframework.dao.DataIntegrityViolationException}
 * OUTSIDE this transaction — the loser's run+items then roll back ATOMICALLY (no orphaned zero-item
 * run) and the caller requeries the winner in a fresh tx via {@link #replay}.
 *
 * <h2>Why NOT a REQUIRES_NEW inner tx on just the run insert (the reverted P1-65 shape)</h2>
 * The run insert is NON-TERMINAL: the classified items are persisted AFTER it. Isolating only the
 * run in a {@code REQUIRES_NEW} inner tx COMMITS it independently, so if the outer tx later fails
 * the run survives durably with ZERO items; a later same-(sourceKey, feedSnapshotHash) re-run
 * short-circuits on {@code findExisting().isPresent()} and returns that empty run — the keys are
 * NEVER classified, permanently. Run + items therefore MUST share one atomic transaction; the
 * {@code REQUIRES_NEW} idiom is correct only for a TERMINAL insert (nothing written after it).
 *
 * <h2>Why a separate bean (not a private method)</h2>
 * Spring's declarative transactions are proxy-based: a call to a sibling method on the SAME service
 * bean bypasses the proxy (the self-invocation trap), so its {@code @Transactional} — and the
 * independent rollback the catch-outside pattern depends on — would silently not apply. Crossing a
 * bean boundary into this dedicated collaborator is mandatory. Precedent in this catalog:
 * {@code eventingest.IngestStateCreator}.
 */
@Component
public class ReconciliationRunCreator {

    private final ReconciliationRunRepository runs;
    private final MemberWriter members;
    private final Clock clock;

    public ReconciliationRunCreator(ReconciliationRunRepository runs, MemberWriter members, Clock clock) {
        this.runs = runs;
        this.members = members;
        this.clock = clock;
    }

    /**
     * Insert the run and persist every classified item as ONE atomic unit ({@code REQUIRED} — joins
     * no outer tx here because {@link ReconciliationService#run} is non-transactional, so this is the
     * unit of atomicity). {@code saveAndFlush} forces a concurrent-identical run's
     * {@code uq(source_key, feed_snapshot_hash)} violation to fire INSIDE this tx, marking it
     * rollback-only so BOTH the run and any items already persisted roll back together; the
     * {@link org.springframework.dao.DataIntegrityViolationException} then propagates PAST this tx
     * boundary for {@code run()} to catch and replay.
     */
    @Transactional
    public ReconciliationRun doRun(String sourceKey, String feedSnapshotHash,
                                   Map<String, BigDecimal> internal, Map<String, BigDecimal> external) {
        Instant now = Instant.now(clock);
        ReconciliationRun saved = runs.saveAndFlush(
            new ReconciliationRun(UUID.randomUUID(), sourceKey, feedSnapshotHash, now));
        // the union of keys, deterministically ordered, each classified exactly once — in the SAME tx.
        TreeSet<String> keys = new TreeSet<>();
        keys.addAll(internal.keySet());
        keys.addAll(external.keySet());
        for (String key : keys) {
            members.persist(new ReconciliationItem(UUID.randomUUID(), saved.getId(), key,
                internal.get(key), external.get(key), now));
        }
        return saved;
    }

    /** Requery the winning run in a FRESH transaction after a lost insert race. */
    @Transactional(readOnly = true)
    public ReconciliationRun replay(String sourceKey, String feedSnapshotHash) {
        return runs.findBySourceKeyAndFeedSnapshotHash(sourceKey, feedSnapshotHash)
            .orElseThrow(ReconciliationException::notFound);
    }
}
