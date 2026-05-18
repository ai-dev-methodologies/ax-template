/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/ScheduledTask
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Entity mapping with @Entity, @Id, @GeneratedValue"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 *   - source_type: external
 *     citation: "ShedLock — distributed lock for Spring scheduled tasks"
 *     url: "https://github.com/lukas-krecan/ShedLock"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   ScheduledTask represents a registered background job with its cron expression and lock state.
 *   Extends BaseEntity (SP13) for: id (UUID), createdAt, updatedAt, deleted.
 *   Lock state (lockHolder, lockedAt) is stored on this entity to support the DB-row lock pattern.
 */
package com.example.app.scheduledtask;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.SQLDelete;

/**
 * Scheduled task registration entity.
 *
 * <p>Represents a named background job that runs on a cron schedule.
 * Execution is guarded by a distributed lock: {@code lockHolder} + {@code lockedAt}
 * implement the DB-row pessimistic lock pattern to prevent concurrent runs.
 *
 * <p>Status lifecycle:
 * <pre>
 *   REGISTERED (created) → ACTIVE (first run attempted) → PAUSED (admin-disabled)
 * </pre>
 *
 * <p>Extends {@code BaseEntity} (SP13) for: id, createdAt, updatedAt, deleted.
 */
@Entity
@SQLDelete(sql = "UPDATE scheduled_tasks SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "scheduled_tasks",
    indexes = {
        @Index(name = "idx_scheduled_tasks_status", columnList = "status"),
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_scheduled_tasks_name", columnNames = "name")
    }
)
public class ScheduledTask extends BaseEntity {

    // ─── identity ──────────────────────────────────────────────────────────

    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;

    @Column(name = "cron_expression", nullable = false, length = 128)
    private String cronExpression;

    // ─── lifecycle ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ScheduledTaskStatus status = ScheduledTaskStatus.REGISTERED;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    // ─── distributed lock state ────────────────────────────────────────────

    /**
     * ID of the node currently holding the lock; null when unlocked.
     * Format: {@code ${spring.application.name}@${hostname}:${pid}} or UUID.
     */
    @Column(name = "lock_holder", length = 256)
    private String lockHolder;

    /**
     * Timestamp when the lock was acquired; null when unlocked.
     * Used for TTL-based stale lock detection (SCHED-LOCK-002).
     */
    @Column(name = "locked_at")
    private Instant lockedAt;

    // ─── constructors ──────────────────────────────────────────────────────

    protected ScheduledTask() {
        // JPA
    }

    /**
     * Factory — creates a new REGISTERED scheduled task.
     *
     * @param name           unique task name (e.g., "cleanup-expired-tokens")
     * @param cronExpression standard cron expression (e.g., "0 0 2 * * ?")
     */
    public static ScheduledTask create(String name, String cronExpression) {
        var task = new ScheduledTask();
        task.name = name;
        task.cronExpression = cronExpression;
        task.status = ScheduledTaskStatus.REGISTERED;
        return task;
    }

    // ─── domain transitions ────────────────────────────────────────────────

    /**
     * Acquires the distributed lock for this task.
     * Caller must verify lock is not already held (or is stale) before calling.
     */
    public void acquireLock(String holder) {
        this.lockHolder = holder;
        this.lockedAt = Instant.now();
        this.status = ScheduledTaskStatus.ACTIVE;
    }

    /**
     * Releases the distributed lock. Sets lockHolder and lockedAt to null.
     */
    public void releaseLock() {
        this.lockHolder = null;
        this.lockedAt = null;
    }

    /**
     * Records the last successful execution time.
     */
    public void recordLastRun(Instant at) {
        this.lastRunAt = at;
    }

    // ─── getters ───────────────────────────────────────────────────────────

    public String getName()                     { return name; }
    public String getCronExpression()           { return cronExpression; }
    public ScheduledTaskStatus getStatus()      { return status; }
    public Instant getLastRunAt()               { return lastRunAt; }
    public String getLockHolder()               { return lockHolder; }
    public Instant getLockedAt()                { return lockedAt; }

    // ─── enum ──────────────────────────────────────────────────────────────

    public enum ScheduledTaskStatus {
        REGISTERED, ACTIVE, PAUSED
    }
}
