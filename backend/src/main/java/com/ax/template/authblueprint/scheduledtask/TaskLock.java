package com.ax.template.authblueprint.scheduledtask;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

/**
 * Row representing one distributed lock.
 * <p>
 * One row per {@code taskName}. Inserted on first {@link LockingPolicy#tryAcquire};
 * updated on stale-lock takeover (SCHED-LOCK-002).
 * <p>
 * Trace: blueprints/scheduled-task-manifest.yaml#lock (strategy=db-row-lock)
 */
@Entity
@Table(name = "task_locks")
public class TaskLock {

    @Id
    @Column(name = "task_name", nullable = false, length = 128)
    private String taskName;

    @Column(name = "lock_holder", nullable = false, length = 255)
    private String lockHolder;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Required by JPA. */
    protected TaskLock() {}

    public TaskLock(String taskName, String lockHolder, Instant lockedAt) {
        this.taskName = Objects.requireNonNull(taskName, "taskName");
        this.lockHolder = Objects.requireNonNull(lockHolder, "lockHolder");
        this.lockedAt = Objects.requireNonNull(lockedAt, "lockedAt");
    }

    public void takeOver(String newHolder, Instant when) {
        this.lockHolder = Objects.requireNonNull(newHolder, "newHolder");
        this.lockedAt = Objects.requireNonNull(when, "when");
    }

    public String getTaskName() { return taskName; }
    public String getLockHolder() { return lockHolder; }
    public Instant getLockedAt() { return lockedAt; }
    public long getVersion() { return version; }
}
