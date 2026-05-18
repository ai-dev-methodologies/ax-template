/**
 * @ax-template-meta
 * template_id: backend/jobs/JobQueue
 * layer: backend-domain
 * domain: jobs
 * anchors_rule: soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Transactional Outbox Pattern (microservices.io) — store the job/message record in the same DB transaction as the domain write; a background poller processes the queue with at-least-once delivery"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — @Entity mapping with @GeneratedValue(strategy = GenerationType.UUID) generates a UUID primary key at the database level via Hibernate 6"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   JobQueue entity represents a single pending background job.
 *   JobWorker polls for PENDING rows and executes them in separate transactions.
 *   Implement JobDispatcher to write JobQueue rows within the domain transaction.
 *   Run the Flyway migration to create the job_queue table before first use.
 */
package com.example.app.jobs;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.type.SqlTypes;

/**
 * Persistent job queue entry — one pending background job.
 *
 * <p>Lifecycle:
 * <pre>
 *   PENDING (dispatched) → RUNNING (worker picks up) → DONE (executed successfully)
 *                                                     → FAILED (execution error, retries exhausted)
 * </pre>
 *
 * <p>The worker locks the row for update before transitioning PENDING → RUNNING,
 * preventing concurrent workers from processing the same job.
 *
 * <p>Payload is stored as JSONB — job handlers deserialise it using the job type's
 * known schema. No schema validation is performed at enqueue time.
 *
 * <p>Extends {@code BaseEntity} for: id (UUID), createdAt, updatedAt, version, deleted_at.
 */
@Entity
@SQLDelete(sql = "UPDATE job_queue SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "job_queue",
    indexes = {
        @Index(name = "idx_job_queue_status_created", columnList = "status, created_at"),
        @Index(name = "idx_job_queue_type_status", columnList = "job_type, status"),
    }
)
public class JobQueue extends BaseEntity {

    /** Stable string identifier matching a registered {@code JobHandler} bean. */
    @Column(name = "job_type", nullable = false, length = 128, updatable = false)
    private String jobType;

    /** Job-specific parameters; schema is job-type–dependent. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private JobStatus status = JobStatus.PENDING;

    /** Number of execution attempts; incremented on each retry. */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    /** Timestamp of the last execution attempt; null if never attempted. */
    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    /** Error message from the last failed attempt; null if never failed. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected JobQueue() {
        // JPA
    }

    /**
     * Factory — creates a new PENDING job queue entry.
     *
     * @param jobType  stable handler identifier
     * @param payload  JSON-serializable parameters
     */
    public static JobQueue create(String jobType, Map<String, Object> payload) {
        var job = new JobQueue();
        job.jobType = jobType;
        job.payload = payload;
        job.status = JobStatus.PENDING;
        job.attemptCount = 0;
        return job;
    }

    // ─── transitions ───────────────────────────────────────────────────────────

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.lastAttemptedAt = Instant.now();
        this.attemptCount++;
    }

    public void markDone() {
        this.status = JobStatus.DONE;
    }

    public void markFailed(String error) {
        this.status = JobStatus.FAILED;
        this.errorMessage = error;
    }

    public void resetToPending() {
        this.status = JobStatus.PENDING;
        this.errorMessage = null;
    }

    // ─── getters ───────────────────────────────────────────────────────────────

    public String getJobType()           { return jobType; }
    public Map<String, Object> getPayload() { return payload; }
    public JobStatus getStatus()         { return status; }
    public int getAttemptCount()         { return attemptCount; }
    public Instant getLastAttemptedAt()  { return lastAttemptedAt; }
    public String getErrorMessage()      { return errorMessage; }

    // ─── enum ──────────────────────────────────────────────────────────────────

    public enum JobStatus {
        PENDING, RUNNING, DONE, FAILED
    }
}
