package com.ax.template.authblueprint.scheduledtask;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Row representing one distributed lock.
 * <p>
 * One row per {@code taskName}. Inserted on first {@link LockingPolicy#tryAcquire};
 * updated on stale-lock takeover (SCHED-LOCK-002).
 * <p>
 * Trace: blueprints/scheduled-task-manifest.yaml#lock (strategy=db-row-lock)
 *
 * <h2>Why {@link Persistable} (BACKLOG P2-48)</h2>
 * {@code taskName} is an ASSIGNED identifier, so it is never null on a brand-new
 * instance. Spring Data's default new-entity test is "id == null" (the {@code @Version}
 * here is a primitive, so it cannot serve as the test either), which means
 * {@code save(new TaskLock(...))} was classified as an UPDATE and routed to
 * {@code EntityManager#merge} — a SELECT-then-UPDATE. That silently disarmed the
 * {@code task_names} PRIMARY KEY as the first-acquire arbiter: a racer whose merge-SELECT
 * observed the winner's just-committed row issued an UPDATE over it and STOLE the lock
 * instead of failing on a duplicate key. Declaring newness explicitly forces
 * {@code persist} — a real INSERT — so the PK decides. Unlike every other idempotent
 * insert in this catalog, this entity cannot rely on a generated id to get that for free.
 */
@AggregateRoot
@Entity
@Table(name = "task_locks")
public class TaskLock implements Persistable<String> {

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

    /**
     * NOT a column. True only between construction and the first flush/load, which is what
     * {@link #isNew()} reports to Spring Data (see the class javadoc).
     */
    @Transient
    private boolean unsaved = true;

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

    /** {@inheritDoc} — the assigned identifier IS {@code taskName}. */
    @Override
    public String getId() { return taskName; }

    /** {@inheritDoc} — see the class javadoc: this is what routes a first acquire to INSERT. */
    @Override
    public boolean isNew() { return unsaved; }

    @PostPersist
    @PostLoad
    void markPersisted() { this.unsaved = false; }

    public String getTaskName() { return taskName; }
    public String getLockHolder() { return lockHolder; }
    public Instant getLockedAt() { return lockedAt; }
    public long getVersion() { return version; }
}
