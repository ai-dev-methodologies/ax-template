package com.ax.template.authblueprint.scheduledtask;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bean wiring for the scheduled-task domain.
 * <p>
 * NOTE: {@code @EnableScheduling} is already declared by
 * {@code com.ax.template.authblueprint.auditlog.AuditLogConfig}; declaring it
 * twice would register the {@code @Scheduled} post-processor twice. The
 * existing {@code auditLogClock} bean is reused for both retention and
 * scheduler bookkeeping — only one {@link java.time.Clock} bean exists in the
 * context so {@code @Autowired Clock} stays unambiguous.
 */
@Configuration
@EnableConfigurationProperties(ScheduledTaskProperties.class)
public class ScheduledTaskConfig {
}
