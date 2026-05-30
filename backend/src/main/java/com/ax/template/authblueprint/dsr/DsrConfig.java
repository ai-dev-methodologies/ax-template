package com.ax.template.authblueprint.dsr;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the DSR domain + enables the {@code @Scheduled} SLA sweep
 * ({@link DsrSlaSweeper}).
 *
 * <p>{@code @EnableScheduling} is declared locally per the per-domain convention
 * used by {@code AuditLogConfig} / {@code ScheduledTaskConfig} / {@code ReportExportConfig}
 * (Spring dedupes the scheduling post-processor). The {@link java.time.Clock} bean
 * is the {@code AuditLogConfig#auditLogClock()} singleton — autowired rather than
 * re-registered to avoid an ambiguous {@code Clock} bean.
 */
@Configuration
@EnableScheduling
public class DsrConfig {
}
