package com.ax.template.authblueprint.timedoffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * TIMEDOFFER-LIFECYCLE-001 + timeout-sweep-is-a-concurrent-mutator. A @Scheduled poller that
 * expires past-deadline OPEN offers EXACTLY ONCE, recording who (SYSTEM) and when — but it is just
 * another concurrent writer: {@link #expireOne} takes the offer's PESSIMISTIC_WRITE row lock and
 * re-checks OPEN + deadline-passed INSIDE its own {@code REQUIRES_NEW} transaction, so a live accept
 * that committed in the gap makes the sweep cleanly LOSE (skip the row). The state-machine
 * transition + @Version write are the defense-in-depth backstop.
 *
 * <p>@Lazy self-injection is load-bearing: a bare in-method {@code expireOne} self-call from the
 * @Scheduled tick would BYPASS the @Transactional proxy (self-invocation), silently dropping REQUIRES_NEW and
 * the row lock on the production path while every tested path kept them. The {@code self} proxy
 * routes the call back through the container; @Lazy breaks the construction cycle.
 *
 * <p>The default delay is long (1h) so the scheduler never interferes with fast tests; tests call
 * {@link #sweepOnce()} directly (the same synchronous path the dispatch/ExportWorker reference uses).
 */
@Component
public class TimedOfferSweeper {

    private static final Logger log = LoggerFactory.getLogger(TimedOfferSweeper.class);
    private static final int SWEEP_BATCH = 500;

    private final TimedOfferService service;
    private final TimedOfferRepository offers;
    private final TimedOfferStateMachine sm;
    private final TimedOfferMetrics metrics;
    private final Clock clock;
    /** Proxy self-reference so the @Scheduled tick reaches {@link #expireOne} THROUGH the proxy. */
    private final TimedOfferSweeper self;

    public TimedOfferSweeper(TimedOfferService service, TimedOfferRepository offers,
                             TimedOfferStateMachine sm, TimedOfferMetrics metrics, Clock clock,
                             @Lazy TimedOfferSweeper self) {
        this.service = service;
        this.offers = offers;
        this.sm = sm;
        this.metrics = metrics;
        this.clock = clock;
        this.self = self;
    }

    @Scheduled(fixedDelayString = "${timed-offer.sweep-interval-ms:3600000}",
               initialDelayString = "${timed-offer.sweep-initial-delay-ms:3600000}")
    public void sweep() {
        sweepOnce();
    }

    /** Synchronous entry (tests + @Scheduled). Returns the number of due rows swept this pass. */
    public int sweepOnce() {
        List<UUID> due = service.dueOfferIds(SWEEP_BATCH);
        for (UUID offerId : due) {
            try {
                self.expireOne(offerId);              // through the proxy → REQUIRES_NEW + row lock
            } catch (RuntimeException ex) {
                // a live accept won the @Version race (OptimisticLock) — skip this row, keep sweeping
                log.debug("sweep skipped offer {} (lost the race or transient): {}", offerId, ex.toString());
            }
        }
        return due.size();
    }

    /** Per-row sweep handler (its OWN transaction, cross-bean from the @Scheduled tick via {@code self}). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOne(UUID offerId) {
        TimedOffer o = offers.findByIdForUpdate(offerId).orElse(null);
        if (o == null) {
            return;                                                       // idempotent: already gone
        }
        Instant now = Instant.now(clock);
        if (o.getStatus() != OfferStatus.OPEN || !o.isPastDeadline(now)) {
            return;                          // a live accept/decline committed in the gap — sweep loses
        }
        sm.expire(o, TimedOfferService.SYSTEM_ACTOR, now);  // EXACTLY ONCE — records SYSTEM + when
        metrics.record("expire", "expired");
    }
}
