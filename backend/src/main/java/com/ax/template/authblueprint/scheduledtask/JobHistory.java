package com.ax.template.authblueprint.scheduledtask;

import com.ax.template.authblueprint.common.PiiSanitized;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Immutable execution audit row — one per task run.
 * <p>
 * Trace:
 * <ul>
 *   <li>SCHED-EXECUTE-001 — every run recorded with startTime + endTime + outcome</li>
 *   <li>blueprints/scheduled-task-manifest.yaml#execute</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(
    name = "job_history",
    indexes = {
        @Index(name = "ix_job_history_task_name", columnList = "task_name"),
        @Index(name = "ix_job_history_started_at", columnList = "started_at")
    }
)
public class JobHistory {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "task_id", updatable = false, nullable = false)
    private UUID taskId;

    @Column(name = "task_name", updatable = false, nullable = false, length = 128)
    private String taskName;

    @Column(name = "started_at", updatable = false, nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private JobOutcome outcome;

    @PiiSanitized(reason =
        "ScheduledTaskService.runOne calls AuditPiiHelper.sanitizeReason(raw) "
        + "before invoking history.markFailure(msg). Entity stores already-scrubbed value.")
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "host_instance", updatable = false, length = 255)
    private String hostInstance;

    /** Required by JPA. */
    protected JobHistory() {}

    private JobHistory(UUID id, UUID taskId, String taskName, Instant startedAt,
                       JobOutcome outcome, String hostInstance) {
        this.id = Objects.requireNonNull(id, "id");
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.taskName = Objects.requireNonNull(taskName, "taskName");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.hostInstance = hostInstance;
    }

    /** Start row — outcome defaults to SUCCESS, overwritten on completion. */
    public static JobHistory start(UUID taskId, String taskName, String hostInstance) {
        return new JobHistory(UUID.randomUUID(), taskId, taskName, Instant.now(),
            JobOutcome.SUCCESS, hostInstance);
    }

    public void markSuccess() {
        this.outcome = JobOutcome.SUCCESS;
        this.finishedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailure(String message) {
        this.outcome = JobOutcome.FAILED;
        this.finishedAt = Instant.now();
        // truncate to column width; never let an oversized stacktrace blow up insert
        if (message != null && message.length() > 2000) {
            this.errorMessage = message.substring(0, 2000);
        } else {
            this.errorMessage = message;
        }
    }

    public void markSkippedLock() {
        this.outcome = JobOutcome.SKIPPED_LOCK;
        this.finishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public String getTaskName() { return taskName; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public JobOutcome getOutcome() { return outcome; }
    public String getErrorMessage() { return errorMessage; }
    public String getHostInstance() { return hostInstance; }
}
