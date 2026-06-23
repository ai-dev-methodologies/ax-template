package com.ax.template.authblueprint.recurringinterval;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * completion-reset-recurring-interval-l0 overdue sweep (CRI-SWEEP-001). A CONCURRENT MUTATOR per
 * timeout-sweep-is-a-concurrent-mutator: {@link #sweepOne} acquires the obligation's
 * PESSIMISTIC_WRITE row exactly like the complete path, DERIVES whether the current window has
 * elapsed, and records ONLY the NON-authoritative {@code sweptOverdue} flag (operational
 * visibility; the authoritative due/overdue is still recomputed on read, CRI-DUE-001). It NEVER
 * completes, advances, or otherwise satisfies an obligation — only a real completion slides the
 * window. The synchronous {@code sweepOne} is the deterministic test path (ObligationSweeper
 * precedent); {@link #sweep} is the production poller.
 */
@Component
public class RecurringIntervalSweeper {

    static final int SWEEP_PAGE = 50;

    private final RecurringObligationRepository obligations;
    private final RecurringIntervalMetrics metrics;
    private final Clock clock;
    /** Proxy self-reference — a bare {@code this.sweepOne(...)} from the @Scheduled tick would
     *  BYPASS the @Transactional proxy (self-invocation), silently dropping REQUIRES_NEW and the
     *  row lock on the production path while every tested path (controller/test) kept them. @Lazy
     *  breaks the construction cycle. */
    private final RecurringIntervalSweeper self;

    public RecurringIntervalSweeper(RecurringObligationRepository obligations,
                                    RecurringIntervalMetrics metrics, Clock clock,
                                    @Lazy RecurringIntervalSweeper self) {
        this.obligations = obligations;
        this.metrics = metrics;
        this.clock = clock;
        this.self = self;
    }

    @Scheduled(fixedDelayString = "${recurring-interval.sweep.fixed-delay-ms:60000}")
    public void sweep() {
        obligations.findByStatusOrderByCreatedAtAsc(RecurringObligationStatus.OPEN,
                PageRequest.of(0, SWEEP_PAGE))
            .forEach(o -> {
                try {
                    self.sweepOne(o.getId());          // through the proxy — REQUIRES_NEW + lock
                } catch (RuntimeException ex) {
                    // ObligationSweeper precedent: a poison/contended obligation must not starve the batch
                    metrics.record("sweep", "rejected");
                }
            });
    }

    /** Record the NON-authoritative overdue flag for ONE obligation. Returns whether it is overdue.
     *  This NEVER completes or advances the obligation — only a real completion slides the window. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sweepOne(UUID obligationId) {
        RecurringObligation o = obligations.findByIdForUpdate(obligationId)
            .orElseThrow(RecurringIntervalException::notFound);
        Instant now = Instant.now(clock);
        boolean overdue = !now.isBefore(o.nextDueAt());    // DERIVED — same predicate the service reads
        o.recordSweptOverdue(overdue);                     // NON-authoritative flag only
        metrics.record("sweep", overdue ? "overdue" : "current");
        return overdue;                                    // NEVER completes / advances the obligation
    }
}
