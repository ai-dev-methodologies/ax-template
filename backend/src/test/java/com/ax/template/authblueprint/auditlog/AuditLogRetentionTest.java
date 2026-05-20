package com.ax.template.authblueprint.auditlog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

/**
 * RETENTION family (3 items).
 * <ul>
 *   <li>AUDIT-RETENTION-001 — cutoff math (purgeOlderThan called with correct cutoff)</li>
 *   <li>AUDIT-RETENTION-002 — three standard tiers (SHORT=30 / STANDARD=90 / LONG=365)</li>
 *   <li>AUDIT-RETENTION-003 — WARN when deleted count meets warning_threshold</li>
 * </ul>
 */
class AuditLogRetentionTest {

    private AuditLogService service;
    private AuditLogProperties properties;
    private Clock fixedClock;
    private AuditLogRetentionJob job;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setup() {
        service = mock(AuditLogService.class);
        properties = new AuditLogProperties();
        properties.getRetention().setWarningThreshold(10_000);
        fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
        job = new AuditLogRetentionJob(service, properties, fixedClock);

        Logger logger = (Logger) LoggerFactory.getLogger(AuditLogRetentionJob.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.ALL);
    }

    @AfterEach
    void teardown() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuditLogRetentionJob.class);
        logger.detachAppender(appender);
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-RETENTION-001")
    void retention_001_cutoffMatchesTierDays() {
        when(service.purgeOlderThan(any(Instant.class))).thenReturn(0);

        job.runForTier(RetentionTier.SHORT);

        var captor = forClass(Instant.class);
        verify(service).purgeOlderThan(captor.capture());

        Instant expected = Instant.parse("2026-05-01T00:00:00Z")
            .minus(RetentionTier.SHORT.daysToKeep(), ChronoUnit.DAYS);
        assertThat(captor.getValue())
            .as("SHORT tier cutoff = now − 30 days")
            .isEqualTo(expected);
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-RETENTION-002")
    void retention_002_threeStandardTiers() {
        assertThat(RetentionTier.SHORT.daysToKeep()).isEqualTo(30);
        assertThat(RetentionTier.STANDARD.daysToKeep()).isEqualTo(90);
        assertThat(RetentionTier.LONG.daysToKeep()).isEqualTo(365);

        // Each tier produces a different cutoff when fed through the job.
        when(service.purgeOlderThan(any(Instant.class))).thenReturn(0);
        for (RetentionTier tier : RetentionTier.values()) {
            job.runForTier(tier);
        }

        var captor = forClass(Instant.class);
        verify(service, org.mockito.Mockito.times(3)).purgeOlderThan(captor.capture());
        assertThat(captor.getAllValues()).hasSize(3);
        assertThat(captor.getAllValues().get(0))
            .isEqualTo(Instant.parse("2026-05-01T00:00:00Z").minus(30, ChronoUnit.DAYS));
        assertThat(captor.getAllValues().get(1))
            .isEqualTo(Instant.parse("2026-05-01T00:00:00Z").minus(90, ChronoUnit.DAYS));
        assertThat(captor.getAllValues().get(2))
            .isEqualTo(Instant.parse("2026-05-01T00:00:00Z").minus(365, ChronoUnit.DAYS));
    }

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-RETENTION-003")
    void retention_003_warnAboveThresholdInfoBelow() {
        properties.getRetention().setWarningThreshold(10_000);

        when(service.purgeOlderThan(any(Instant.class))).thenReturn(15_000);
        job.runForTier(RetentionTier.STANDARD);
        assertThat(hasLevel(Level.WARN))
            .as("WARN must be emitted when deleted=15000 >= threshold=10000")
            .isTrue();

        appender.list.clear();

        when(service.purgeOlderThan(any(Instant.class))).thenReturn(500);
        job.runForTier(RetentionTier.STANDARD);
        assertThat(hasLevel(Level.WARN))
            .as("WARN must NOT be emitted when deleted=500 < threshold=10000")
            .isFalse();
        assertThat(hasLevel(Level.INFO))
            .as("INFO must be emitted when deleted=500 < threshold=10000")
            .isTrue();
    }

    private boolean hasLevel(Level level) {
        return appender.list.stream().anyMatch(e -> e.getLevel().equals(level));
    }
}
