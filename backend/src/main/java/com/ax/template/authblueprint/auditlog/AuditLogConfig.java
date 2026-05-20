package com.ax.template.authblueprint.auditlog;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Wires audit-log beans.
 * <p>
 * Trace: AUDIT-RETENTION-001 (scheduling) + AUDIT-PII-001 (properties).
 */
@Configuration
@EnableConfigurationProperties(AuditLogProperties.class)
@EnableScheduling
public class AuditLogConfig {

    @Bean
    Clock auditLogClock() {
        return Clock.systemUTC();
    }
}
