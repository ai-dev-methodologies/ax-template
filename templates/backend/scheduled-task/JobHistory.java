/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/JobHistory
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Entity mapping with @Entity, @Id, @GeneratedValue"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 *   - source_type: external
 *     citation: "12-Factor App — Factor XI: Logs (treat logs as event streams)"
 *     url: "https://12factor.net/logs"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   JobHistory records one execution attempt of a ScheduledTask.
 *   Every run (success or failure) is recorded per SCHED-EXECUTE-001.
 *   Extends BaseEntity (SP13) for: id (UUID), createdAt, updatedAt, deleted.
 */
package com.example.app.scheduledtask;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Job history record — one execution attempt of a ScheduledTask.
 *
 * <p>Lifecycle:
 * <pre>
 *   RUNNING (started) → SUCCESS (completed normally)
 *                     → FAILED (exception thrown)
 * </pre>
 *
 * <p>Created before task execution; updated in the finally block regardless of outcome (SCHED-EXECUTE-001).
 *
 * <p>Extends {@code BaseEntity} (SP13) for: id, createdAt, updatedAt, deleted.
 */
@Entity
@Table(
    name = "job_history",
    indexes = {
        @Index(name = "idx_job_history_task_started", columnList = "task_id, started_at"),
        @Index(name = "idx_job_history_started_at", columnList = "started_at"),
    }
)
public class JobHistory extends BaseEntity {

    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "task_name", nullable = false, updatable = false, length = 128)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private JobStatus status = JobStatus.RUNNING;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected JobHistory() {
        // JPA
    }

    /**
     * Factory — creates a RUNNING job history record.
     *
     * @param taskId   UUID of the ScheduledTask being executed
     * @param taskName name of the task (for display without joins)
     */
    public static JobHistory start(UUID taskId, String taskName) {
        var h = new JobHistory();
        h.taskId = taskId;
        h.taskName = taskName;
        h.status = JobStatus.RUNNING;
        h.startedAt = Instant.now();
        return h;
    }

    // ─── domain transitions ────────────────────────────────────────────────

    /**
     * Marks this history record as SUCCESS with the completion timestamp.
     */
    public void markSuccess(Instant finishedAt) {
        this.status = JobStatus.SUCCESS;
        this.finishedAt = finishedAt;
    }

    /**
     * Marks this history record as FAILED with the exception message.
     */
    public void markFailure(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.finishedAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    // ─── getters ───────────────────────────────────────────────────────────

    public UUID getTaskId()         { return taskId; }
    public String getTaskName()     { return taskName; }
    public JobStatus getStatus()    { return status; }
    public Instant getStartedAt()   { return startedAt; }
    public Instant getFinishedAt()  { return finishedAt; }
    public String getErrorMessage() { return errorMessage; }

    // ─── enum ──────────────────────────────────────────────────────────────

    public enum JobStatus {
        RUNNING, SUCCESS, FAILED
    }
}
