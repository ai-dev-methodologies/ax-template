package com.ax.template.authblueprint.recurringinterval;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * completion-reset-recurring-interval-l0 sole orchestrator. The defining property (CRI-RESET-001):
 * completing the current occurrence appends an immutable {@link Occurrence} AND advances the
 * obligation's {@code windowStart} to the completion instant, so the NEXT window is measured FROM
 * the completion — doing the task early slides the whole future schedule forward (vs a fixed grid).
 * Every write path takes the obligation's PESSIMISTIC_WRITE row lock (CRI-CONCURRENT-001), so across
 * N concurrent completes on the same window EXACTLY ONE advances and the rest are a deterministic
 * 409 (the {@code uq(obligation_id, closed_window_start)} backstop makes the loser deterministic,
 * CWE-362). Due/overdue is ALWAYS recomputed from the clock + windowStart (CRI-DUE-001) — never a
 * stored authoritative boolean. Occurrences are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class RecurringObligationService {

    static final int MAX_PAGE_SIZE = 200;

    private final RecurringObligationRepository obligations;
    private final MemberWriter members;
    private final RecurringIntervalMetrics metrics;
    private final Clock clock;

    public RecurringObligationService(RecurringObligationRepository obligations, MemberWriter members,
                                      RecurringIntervalMetrics metrics, Clock clock) {
        this.obligations = obligations;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** Create a recurring obligation. The first window starts at {@code anchorAt} (or now). No API
     *  accepts a windowStart-derived due date — the window is computed from anchor + interval. */
    @Transactional
    public RecurringObligation create(String obligationKey, long intervalSeconds, Instant anchorAt) {
        if (intervalSeconds <= 0) {
            metrics.record("create", "invalid");
            throw RecurringIntervalException.invalidInterval();
        }
        if (obligations.existsByObligationKey(obligationKey)) {
            metrics.record("create", "rejected");
            throw RecurringIntervalException.duplicateKey();
        }
        Instant now = Instant.now(clock);
        Instant windowStart = anchorAt == null ? now : anchorAt;
        try {
            RecurringObligation o = new RecurringObligation(UUID.randomUUID(), obligationKey,
                intervalSeconds, windowStart, now);
            obligations.saveAndFlush(o);
            metrics.record("create", "ok");
            return o;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw RecurringIntervalException.duplicateKey();
        }
    }

    /** CRI-RESET-001 / CRI-ONCE-001 / CRI-CONCURRENT-001 — complete the current occurrence: append
     *  the immutable Occurrence for the CURRENT window (uq backstop = exactly-once) and advance the
     *  window to the completion instant, all under the obligation's row lock. */
    @Transactional
    public RecurringObligation complete(String obligationKey, String completedBy) {
        if (completedBy == null || completedBy.isBlank()) {
            metrics.record("complete", "invalid");
            throw RecurringIntervalException.completerRequired();
        }
        RecurringObligation o = obligations.findByObligationKeyForUpdate(obligationKey)
            .orElseThrow(RecurringIntervalException::notFound);
        Instant now = Instant.now(clock);
        // CRI-ONCE-001 — at most one completion per window OCCUPANCY. A completion opens the next
        // window [completedAt, completedAt + interval); completing it AGAIN before that window has
        // elapsed (become due again) is a duplicate of the current cycle → 409. Under the row lock
        // this serializes N concurrent completes so EXACTLY ONE advances (the first, on the open
        // window) and the rest observe the just-opened, not-yet-elapsed window → 409.
        if (o.getLastCompletedAt() != null && now.isBefore(o.nextDueAt())) {
            metrics.record("complete", "conflict");
            throw RecurringIntervalException.windowAlreadyCompleted();
        }
        try {
            // uq(obligation_id, closed_window_start) — the DB backstop if the lock ever slipped:
            // appending the SAME window's start twice is impossible (CWE-362).
            members.persistAndFlush(new Occurrence(UUID.randomUUID(), o.getId(),
                o.getWindowStart(), completedBy.strip(), now));
        } catch (DataIntegrityViolationException dup) {
            metrics.record("complete", "conflict");
            throw RecurringIntervalException.windowAlreadyCompleted();
        }
        o.completeAndAdvance(now);                 // windowStart := now (reset, not a fixed grid)
        metrics.record("complete", "advanced");
        return o;
    }

    /** CRI-DUE-001 — the AUTHORITATIVE due/overdue, recomputed from the clock + windowStart on
     *  every read. Never a stored boolean. overdue iff now >= windowStart + intervalSeconds. */
    public boolean isOverdue(RecurringObligation o, Instant now) {
        return !now.isBefore(o.nextDueAt());
    }

    public boolean isOverdueNow(RecurringObligation o) {
        return isOverdue(o, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public RecurringObligation get(String obligationKey) {
        return obligations.findByObligationKey(obligationKey)
            .orElseThrow(RecurringIntervalException::notFound);
    }

    @Transactional(readOnly = true)
    public long occurrenceCount(String obligationKey) {
        return obligations.countOccurrences(get(obligationKey).getId());
    }

    @Transactional(readOnly = true)
    public Page<Occurrence> occurrences(String obligationKey, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return obligations.findOccurrencesPage(get(obligationKey).getId(),
            PageRequest.of(safePage, safeSize));
    }
}
