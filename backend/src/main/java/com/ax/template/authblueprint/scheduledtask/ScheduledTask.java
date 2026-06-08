package com.ax.template.authblueprint.scheduledtask;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Registered scheduled task definition.
 * <p>
 * Trace:
 * <ul>
 *   <li>SCHED-REGISTER-001 — persists name + cron + status=REGISTERED + generated UUID</li>
 *   <li>blueprints/scheduled-task-manifest.yaml#register</li>
 * </ul>
 *
 * <p>{@code lastRunAt} is updated only on successful execution
 * (SCHED-EXECUTE-001 contract: {@code ScheduledTask.lastRun} is touched after
 * a successful run, while {@link JobHistory} records every cycle).
 */
@AggregateRoot
@Entity
@Table(
    name = "scheduled_tasks",
    indexes = {
        @Index(name = "ix_scheduled_tasks_name", columnList = "name", unique = true),
        @Index(name = "ix_scheduled_tasks_status", columnList = "status")
    }
)
public class ScheduledTask {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "cron_expression", nullable = false, length = 128)
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ScheduledTaskStatus status;

    @Column(name = "handler_bean", length = 255)
    private String handlerBean;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Required by JPA. */
    protected ScheduledTask() {}

    private ScheduledTask(UUID id, String name, String cronExpression,
                          ScheduledTaskStatus status, String handlerBean,
                          Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.cronExpression = Objects.requireNonNull(cronExpression, "cronExpression");
        this.status = Objects.requireNonNull(status, "status");
        this.handlerBean = handlerBean;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Factory: SCHED-REGISTER-001 — status defaults to {@link ScheduledTaskStatus#REGISTERED}.
     */
    public static ScheduledTask create(String name, String cronExpression) {
        Instant now = Instant.now();
        return new ScheduledTask(UUID.randomUUID(), name, cronExpression,
            ScheduledTaskStatus.REGISTERED, null, now, now);
    }

    public static ScheduledTask create(String name, String cronExpression, String handlerBean) {
        Instant now = Instant.now();
        return new ScheduledTask(UUID.randomUUID(), name, cronExpression,
            ScheduledTaskStatus.REGISTERED, handlerBean, now, now);
    }

    public void enable() {
        this.status = ScheduledTaskStatus.ENABLED;
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.status = ScheduledTaskStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public void markLastRun(Instant when) {
        this.lastRunAt = when;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCronExpression() { return cronExpression; }
    public ScheduledTaskStatus getStatus() { return status; }
    public String getHandlerBean() { return handlerBean; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getLastRunAt() { return lastRunAt; }
    public long getVersion() { return version; }
}
