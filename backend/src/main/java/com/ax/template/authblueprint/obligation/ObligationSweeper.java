package com.ax.template.authblueprint.obligation;

import com.ax.template.authblueprint.common.MemberWriter;

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
 * deadline-obligation-l0 escalation sweep (OBL-LADDER-001). A CONCURRENT MUTATOR per
 * timeout-sweep-is-a-concurrent-mutator: {@link #processOne} acquires the obligation's
 * PESSIMISTIC_WRITE row exactly like the API paths, fires each due rung AT MOST ONCE (in-code
 * check + UNIQUE(obligation_id, rung) DB backstop), in ladder order, as appended additive
 * events — and NEVER writes the terminal state (OBL-ACK-001: only a human closes the loop).
 * The synchronous {@code processOne} is the deterministic test path (ExportWorker precedent);
 * {@link #sweep} is the production poller.
 */
@Component
public class ObligationSweeper {

    static final int SWEEP_PAGE = 50;

    private final ObligationRepository obligations;
    private final MemberWriter members;
    private final ObligationMetrics metrics;
    private final Clock clock;
    /** Proxy self-reference — a bare {@code this.processOne(...)} from the @Scheduled tick would
     *  BYPASS the @Transactional proxy (self-invocation), silently dropping REQUIRES_NEW and the
     *  row lock on the production path while every tested path (controller/test, external calls)
     *  kept them. @Lazy breaks the construction cycle. */
    private final ObligationSweeper self;

    public ObligationSweeper(ObligationRepository obligations, MemberWriter members,
                             ObligationMetrics metrics, Clock clock, @Lazy ObligationSweeper self) {
        this.obligations = obligations;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
        this.self = self;
    }

    @Scheduled(fixedDelayString = "${obligation.sweep.fixed-delay-ms:60000}")
    public void sweep() {
        obligations.findByStatusOrderByCreatedAtAsc(ObligationStatus.OPEN, PageRequest.of(0, SWEEP_PAGE))
            .forEach(o -> {
                try {
                    self.processOne(o.getId());          // through the proxy — REQUIRES_NEW + lock
                } catch (RuntimeException ex) {
                    // ExportWorker precedent: a poison/contended obligation must not starve the batch
                    metrics.record("sweep", "rejected");
                }
            });
    }

    /** Fire every due-but-unfired rung for ONE obligation, in order. Returns the count fired. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processOne(UUID obligationId) {
        Obligation o = obligations.findByIdForUpdate(obligationId)
            .orElseThrow(ObligationException::notFound);
        if (o.getStatus() == ObligationStatus.ACKNOWLEDGED) {
            metrics.record("sweep", "skipped");           // the loop is closed — never resurrect
            return 0;
        }
        Instant now = Instant.now(clock);
        int fired = 0;
        for (EscalationRung rung : EscalationRung.LADDER) {
            boolean due = !now.isBefore(rung.dueAt(o.getWindowStart(), o.getEffectiveDeadline()));
            if (due && !obligations.rungFired(o.getId(), rung)) {
                members.persist(new EscalationEvent(UUID.randomUUID(), o.getId(), rung, now,
                    o.getEffectiveDeadline()));
                metrics.record("sweep", "fired");
                fired++;
            }
        }
        bindConsequenceIfDue(o, now);                      // OBL-CONSEQUENCE-001/OBL-WAIVER-001 — additive, never touches the ladder above
        return fired;                                     // NEVER closes/expires the obligation
    }

    /** OBL-CONSEQUENCE-001 — checked on EVERY pass past the deadline (not only the pass that fires
     *  BREACH), so a waiver's expiry on EITHER axis reactivates enforcement on the very next sweep
     *  (OBL-WAIVER-001). Idempotent: the existence check + the entity's own UNIQUE(obligation_id)
     *  backstop make a second bind impossible even under a racing re-sweep. */
    private void bindConsequenceIfDue(Obligation o, Instant now) {
        if (o.getBreachBasisAmount() == null || now.isBefore(o.getEffectiveDeadline())) {
            return;
        }
        if (obligations.findConsequence(o.getId()).isPresent()) {
            return;                                        // already bound — exactly-once holds
        }
        boolean waived = obligations.findWaivers(o.getId()).stream()
            .anyMatch(w -> !obligations.isRevoked(w.getId()) && w.isValidAt(now, o.getUsageCycleCount()));
        if (waived) {
            metrics.record("sweep", "waived");             // an active waiver suppresses ONLY the consequence
            return;
        }
        members.persist(new BreachConsequence(UUID.randomUUID(), o.getId(), now, o.getBreachBasisAmount(),
            o.getEffectiveDeadline()));
        metrics.record("sweep", "consequence-bound");
    }
}
