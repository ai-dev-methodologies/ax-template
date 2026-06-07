package com.ax.template.authblueprint.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * RSV-SWEEP-001 + timeout-sweep-is-a-concurrent-mutator. A @Scheduled poller that reclaims stranded
 * (abandoned-session) holds, but is just another concurrent writer: it asks the service for the
 * bounded batch of due OUTSTANDING holds, then delegates EACH to {@link ReservationService#expireOne}
 * — a CROSS-BEAN call so the {@code REQUIRES_NEW} per-row transaction is honored (a self-invocation
 * would silently ignore it). A hold whose live settle/release committed in the gap makes the per-row
 * re-read see a terminal state and skip; we also catch per row so one conflicted row never aborts the
 * sweep. Repository access stays inside the service (layer boundary).
 *
 * <p>The default delay is long (1h) so the scheduler never interferes with fast tests; tests call
 * {@link #sweepOnce()} directly (the same synchronous path the DispatchSweeper reference uses).
 */
@Component
public class ReservationSweeper {

    private static final Logger log = LoggerFactory.getLogger(ReservationSweeper.class);
    private static final int SWEEP_BATCH = 500;

    private final ReservationService reservations;

    public ReservationSweeper(ReservationService reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${reservation.sweep-interval-ms:3600000}",
               initialDelayString = "${reservation.sweep-initial-delay-ms:3600000}")
    public void sweep() {
        sweepOnce();
    }

    /** Synchronous entry (tests + @Scheduled). Returns the number of due holds swept this pass. */
    public int sweepOnce() {
        List<UUID> due = reservations.dueHoldIds(SWEEP_BATCH);
        for (UUID holdId : due) {
            try {
                reservations.expireOne(holdId);     // cross-bean → REQUIRES_NEW per row
            } catch (RuntimeException ex) {
                // a live settle/release won the @Version/status race — skip this row, keep sweeping
                log.debug("sweep skipped reservation {} (lost the race or transient): {}", holdId, ex.toString());
            }
        }
        return due.size();
    }
}
