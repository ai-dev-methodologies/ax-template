package com.ax.template.authblueprint.scheduledtask;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable parameters for the scheduled-task domain.
 * <p>
 * Trace: blueprints/scheduled-task-manifest.yaml#lock (lock_ttl_seconds=300).
 */
@ConfigurationProperties(prefix = "ax.scheduler")
public class ScheduledTaskProperties {

    /** SCHED-LOCK-002 — stale lock TTL in seconds (default 300 = 5 min). */
    private int lockTtlSeconds = 300;

    /** Instance identifier used as lock holder (defaults to a random UUID). */
    private String instanceId = "instance-" + java.util.UUID.randomUUID();

    public int getLockTtlSeconds() { return lockTtlSeconds; }
    public void setLockTtlSeconds(int lockTtlSeconds) { this.lockTtlSeconds = lockTtlSeconds; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
}
