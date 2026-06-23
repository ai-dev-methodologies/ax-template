package com.ax.template.authblueprint.mandate;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * mandate-fanout-l0 deemed-default sweep (MANDATE-DEEMED-001). A plain @Scheduled POLLER: it finds
 * PENDING tasks whose deadline has passed and drives the @Transactional worker
 * {@link MandateService#resolveDeemed(UUID)} per task. The worker lives on a SEPARATE bean
 * ({@link MandateService}, the sole orchestrator), so this cross-bean call already crosses the
 * Spring proxy — the worker's @Transactional boundary + the task-row PESSIMISTIC_WRITE lock are
 * NOT bypassed (the dunning/obligation self-invocation lesson: a bare same-bean this.worker(...)
 * WOULD bypass the proxy; an @Lazy self-reference would be needed only if the worker lived here).
 * The worker is idempotent + lock-serialized, so a racing explicit complete and this sweep
 * converge to exactly one terminal resolution per task (MANDATE-CONCURRENT-001 / CWE-362).
 */
@Component
public class MandateDeemedSweeper {

    static final int SWEEP_PAGE = 100;

    private final MandateRepository mandates;
    private final MandateService service;
    private final MandateMetrics metrics;
    private final Clock clock;

    public MandateDeemedSweeper(MandateRepository mandates, MandateService service,
                                MandateMetrics metrics, Clock clock) {
        this.mandates = mandates;
        this.service = service;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${mandate.deemed-sweep.fixed-delay-ms:60000}")
    public void sweep() {
        Instant now = Instant.now(clock);
        for (UUID taskId : mandates.findOverduePendingTaskIds(now, PageRequest.of(0, SWEEP_PAGE))) {
            try {
                service.resolveDeemed(taskId);          // cross-bean → proxied @Transactional + lock
            } catch (RuntimeException ex) {
                // a poison/contended task must not starve the batch (ExportWorker/ObligationSweeper precedent)
                metrics.record("deemed", "rejected");
            }
        }
    }
}
