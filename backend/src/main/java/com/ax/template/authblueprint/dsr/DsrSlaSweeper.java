package com.ax.template.authblueprint.dsr;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DSR-SLA-001 — @Scheduled sweep that flags still-open requests at/over their
 * due date as SLA-breaching (and increments {@code dsr_sla_breach_total}).
 *
 * <p>The actual work is in {@link DsrService#sweepSlaBreaches()} (transactional,
 * directly callable from tests so assertions do not depend on the poll cadence).
 * Mirrors the {@code ExportWorker} / {@code ScheduledTaskLoop} convention.
 */
@Component
public class DsrSlaSweeper {

    private final DsrService service;

    public DsrSlaSweeper(DsrService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${ax.dsr.sla-sweep-interval-ms:3600000}",
               initialDelayString = "${ax.dsr.sla-sweep-initial-delay-ms:3600000}")
    public void sweep() {
        service.sweepSlaBreaches();
    }
}
