package com.ax.template.authblueprint.auditlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled purge job.
 * <p>
 * Trace:
 * <ul>
 *   <li>AUDIT-RETENTION-001 — purge entries past their retention cutoff</li>
 *   <li>AUDIT-RETENTION-003 — daily cron + WARN above {@code warning_threshold}</li>
 * </ul>
 *
 * <p>This implementation purges using the per-tier maximum (LONG = 365d) as a
 * coarse global guard. A production deployment that needs per-resource-type
 * cutoffs replaces this with a multi-pass that iterates over
 * {@code retention.resource.*} and applies the matching tier's cutoff.
 */
@Component
public class AuditLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionJob.class);

    private final AuditLogService service;
    private final AuditLogProperties properties;
    private final Clock clock;

    public AuditLogRetentionJob(AuditLogService service,
                                AuditLogProperties properties,
                                Clock clock) {
        this.service = service;
        this.properties = properties;
        this.clock = clock;
    }

    /** Daily at 02:00 UTC (manifest retention.purge_schedule.cron). */
    @Scheduled(cron = "${audit.retention.cron:0 0 2 * * *}", zone = "UTC")
    public void runDaily() {
        runForTier(properties.getRetention().getDefaultTier());
    }

    /** Public for unit-test invocation. */
    public int runForTier(RetentionTier tier) {
        Instant cutoff = Instant.now(clock).minus(tier.daysToKeep(), ChronoUnit.DAYS);
        int deleted = service.purgeOlderThan(cutoff);
        int warningThreshold = properties.getRetention().getWarningThreshold();
        if (deleted >= warningThreshold) {
            log.warn("audit-log retention purge deleted={} tier={} threshold={} cutoff={}",
                deleted, tier, warningThreshold, cutoff);
        } else {
            log.info("audit-log retention purge deleted={} tier={} cutoff={}",
                deleted, tier, cutoff);
        }
        return deleted;
    }
}
