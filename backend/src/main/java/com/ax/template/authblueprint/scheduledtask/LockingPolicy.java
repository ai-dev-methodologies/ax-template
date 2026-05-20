package com.ax.template.authblueprint.scheduledtask;

/**
 * Distributed lock SPI for scheduled task coordination.
 * <p>
 * Trace:
 * <ul>
 *   <li>SCHED-LOCK-001 — {@link #tryAcquire(String, String)} returns false when the lock is held; the executor MUST skip execution</li>
 *   <li>SCHED-LOCK-002 — stale locks (held beyond {@code lock_ttl_seconds}) MUST be re-acquirable; implementations check {@code lockedAt + TTL < now()}</li>
 *   <li>SCHED-IDEMPOTENT-001 — atomic acquire guarantees at-most-one execution per TTL window across concurrent callers</li>
 *   <li>blueprints/scheduled-task-manifest.yaml#lock</li>
 * </ul>
 *
 * <p>The default {@link DatabaseAdvisoryLock} implementation uses an
 * INSERT-or-stale-UPDATE row pattern (portable across H2 + Postgres). Production
 * deployments can swap to Redis Redlock / Zookeeper / Shedlock by replacing the
 * {@code LockingPolicy} bean — no domain code change required.
 */
public interface LockingPolicy {

    /**
     * Atomically acquire the lock for {@code taskName} on behalf of
     * {@code lockHolder}. Returns {@code true} iff the caller now owns the lock
     * (no concurrent holder, or previous holder's lock is stale per TTL).
     */
    boolean tryAcquire(String taskName, String lockHolder);

    /** Release the lock if currently held by {@code lockHolder}. */
    void release(String taskName, String lockHolder);
}
