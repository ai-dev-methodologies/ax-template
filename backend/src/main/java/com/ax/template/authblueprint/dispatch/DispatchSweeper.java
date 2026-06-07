package com.ax.template.authblueprint.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * AVAIL-SWEEP-001 + timeout-sweep-is-a-concurrent-mutator. A @Scheduled poller that drives the
 * timeout transitions, but is just another concurrent writer: it asks the service for the bounded
 * batch of due PENDING offers, then delegates EACH row to {@link DispatchService#expireOneOffer} —
 * a CROSS-BEAN call so the {@code REQUIRES_NEW} per-row transaction is honored (a self-invocation
 * would silently ignore it). A row whose live accept committed in the gap makes the per-row
 * @Version write lose (OptimisticLockException); we catch per row so one conflicted row never
 * aborts the sweep. Repository access stays inside the service (layer boundary).
 *
 * <p>The default delay is long (1h) so the scheduler never interferes with fast tests; tests call
 * {@link #sweepOnce()} directly (the same synchronous path the ExportWorker reference uses).
 */
@Component
public class DispatchSweeper {

    private static final Logger log = LoggerFactory.getLogger(DispatchSweeper.class);
    private static final int SWEEP_BATCH = 500;

    private final DispatchService dispatch;

    public DispatchSweeper(DispatchService dispatch) {
        this.dispatch = dispatch;
    }

    @Scheduled(fixedDelayString = "${dispatch.sweep-interval-ms:3600000}",
               initialDelayString = "${dispatch.sweep-initial-delay-ms:3600000}")
    public void sweep() {
        sweepOnce();
    }

    /** Synchronous entry (tests + @Scheduled). Returns the number of due rows swept this pass. */
    public int sweepOnce() {
        List<UUID> due = dispatch.dueOfferIds(SWEEP_BATCH);
        for (UUID offerId : due) {
            try {
                dispatch.expireOneOffer(offerId);     // cross-bean → REQUIRES_NEW per row
            } catch (RuntimeException ex) {
                // a live accept won the @Version race (OptimisticLock) — skip this row, keep sweeping
                log.debug("sweep skipped offer {} (lost the race or transient): {}", offerId, ex.toString());
            }
        }
        return due.size();
    }
}
