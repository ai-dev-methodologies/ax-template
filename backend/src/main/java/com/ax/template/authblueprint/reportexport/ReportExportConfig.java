package com.ax.template.authblueprint.reportexport;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires report-export beans + enables the @Scheduled poller for {@link ExportWorker}.
 *
 * <p>{@code @EnableScheduling} is declared locally per the per-domain convention used
 * by {@code AuditLogConfig} and {@code ScheduledTaskConfig}. The {@link java.time.Clock}
 * bean is provided by {@code AuditLogConfig#auditLogClock()} — we autowire that
 * singleton rather than registering a second {@code Clock} bean (which would create
 * an ambiguous {@code NoUniqueBeanDefinitionException} for existing callers).
 */
@Configuration
@EnableConfigurationProperties(ReportExportProperties.class)
@EnableScheduling
public class ReportExportConfig {
}
