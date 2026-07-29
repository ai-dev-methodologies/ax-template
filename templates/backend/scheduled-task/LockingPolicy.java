/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/LockingPolicy
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule_absent: distributed-lock port; the catalog states no lock invariant. The SECOND
 *   reason recorded here — that DbRowLockingPolicy.tryAcquire was findById -> check -> save
 *   (read-then-write), so anchoring it to shared-counter-claim-must-be-atomic would have made the
 *   template violate its own anchor — no longer holds: BACKLOG P2-48 replaced that read with a
 *   pessimistic SELECT ... FOR UPDATE (ScheduledTaskRepository.findByIdForUpdate), so the claim
 *   and the code now agree. The exemption stands on the first reason ALONE, and re-anchoring is
 *   now a live option rather than a self-violation. Enumerated in JAVA_NO_ANCHOR_EXEMPT in
 *   practices/evals/evidence_guard.sh.
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "ShedLock — distributed lock for Spring scheduled tasks"
 *     url: "https://github.com/lukas-krecan/ShedLock"
 *   - source_type: external
 *     citation: "Baeldung — Distributed Locking with DB-Row Locks in Spring"
 *     url: "https://www.baeldung.com/spring-scheduled-tasks"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   LockingPolicy is the interface for distributed lock acquisition and release.
 *   DbRowLockingPolicy implements it by loading the scheduled_tasks row under a pessimistic
 *   SELECT ... FOR UPDATE row lock (ScheduledTaskRepository.findByIdForUpdate), so the
 *   free-or-stale test and the claiming write cannot interleave with another node's.
 *   Stale locks (lockedAt + TTL < now) are forcibly reclaimed (SCHED-LOCK-002).
 *   Use MockLockingPolicy in unit tests to control lock behavior.
 */
package com.example.app.scheduledtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Distributed lock abstraction for scheduled task execution.
 *
 * <p>Implementations must guarantee that only one node at a time holds the lock for a
 * given task. The free-or-stale TEST and the claiming WRITE must be one indivisible
 * step — a plain read, test, then save lets two nodes pass the same test on the same
 * row and both believe they hold the lock. {@link DbRowLockingPolicy} gets that from a
 * pessimistic row lock ({@code SELECT ... FOR UPDATE}); a conditional single-statement
 * {@code UPDATE … WHERE <still-free>} whose affected-row count decides the winner is
 * equally valid. ({@code SKIP LOCKED} is NOT the tool here: it makes a locked row look
 * absent to the loser instead of making it wait and re-read, and H2 does not support it.)
 *
 * <p>Contract:
 * <ul>
 *   <li>{@link #tryAcquire}: returns true if lock acquired; false if already held. A lost
 *       race MUST be reported by returning false, never by throwing — callers treat an
 *       exception as a failed job, not as a skipped one.
 *   <li>{@link #release}: releases the lock; safe to call even if not held.
 *   <li>Stale locks (held by crashed nodes) are reclaimed after {@code lock_ttl_seconds}.
 * </ul>
 */
public interface LockingPolicy {

    /**
     * Attempts to acquire the distributed lock for the given task.
     *
     * @param taskId     UUID of the task
     * @param lockHolder string identifying this node (e.g., hostname:pid)
     * @return true if lock was acquired; false if already held by another node
     */
    boolean tryAcquire(UUID taskId, String lockHolder);

    /**
     * Releases the distributed lock for the given task.
     * No-op if the lock is not currently held.
     *
     * @param taskId UUID of the task
     */
    void release(UUID taskId);

    // ─── DbRowLockingPolicy ────────────────────────────────────────────────

    /**
     * DB-row based distributed lock using pessimistic locking on the scheduled_tasks table.
     *
     * <p>Acquire: loads the task row with {@code SELECT ... FOR UPDATE}
     * ({@link ScheduledTaskRepository#findByIdForUpdate}), checks whether the lock is free
     * or stale (TTL expired), then sets lockHolder + lockedAt. Holding the row lock across
     * that pair is what makes the acquire atomic: a second node's acquire BLOCKS on the lock
     * and, once granted, re-reads the winner's committed lockedAt — no longer stale — and
     * returns false.
     *
     * <p>Stale lock reclaim: if lockedAt + TTL &lt; now, the lock is considered stale
     * (held by a crashed node) and may be forcibly reclaimed (SCHED-LOCK-002).
     */
    @Component
    class DbRowLockingPolicy implements LockingPolicy {

        private static final Logger log = LoggerFactory.getLogger(DbRowLockingPolicy.class);

        private final ScheduledTaskRepository taskRepository;
        private final Duration lockTtl;

        public DbRowLockingPolicy(
                ScheduledTaskRepository taskRepository,
                @Value("${ax.scheduler.lock-ttl-seconds:300}") long lockTtlSeconds) {
            this.taskRepository = taskRepository;
            this.lockTtl = Duration.ofSeconds(lockTtlSeconds);
        }

        @Override
        @Transactional
        public boolean tryAcquire(UUID taskId, String lockHolder) {
            // FOR UPDATE, not findById: the row lock must span the isLockHeld() test and the
            // acquireLock() write below, or two nodes both pass the test and both "win".
            return taskRepository.findByIdForUpdate(taskId).map(task -> {
                // Check if lock is free or stale
                if (isLockHeld(task)) {
                    log.debug("Lock held by {} — skipping taskId={}", task.getLockHolder(), taskId);
                    return false;
                }
                if (task.getLockHolder() != null) {
                    log.warn("Reclaiming stale lock from {} for taskId={}", task.getLockHolder(), taskId);
                }
                task.acquireLock(lockHolder);
                taskRepository.save(task);
                return true;
            }).orElse(false);
        }

        @Override
        @Transactional
        public void release(UUID taskId) {
            taskRepository.findById(taskId).ifPresent(task -> {
                task.releaseLock();
                taskRepository.save(task);
            });
        }

        /** Returns true if the lock is currently held by an active (non-stale) holder. */
        private boolean isLockHeld(ScheduledTask task) {
            if (task.getLockHolder() == null || task.getLockedAt() == null) {
                return false;
            }
            // Lock is stale if it was acquired longer ago than the TTL
            return task.getLockedAt().plus(lockTtl).isAfter(Instant.now());
        }
    }

    // ─── MockLockingPolicy ─────────────────────────────────────────────────

    /**
     * Test double for LockingPolicy.
     *
     * <p>Default: always grants the lock. Configure via {@link #setGrantLock(boolean)}.
     *
     * <pre>
     *   var mock = new LockingPolicy.MockLockingPolicy();
     *   mock.setGrantLock(false);   // simulate lock held by another node
     *   service.runDueTasksLoop();
     *   assertThat(mock.acquireCallCount()).isEqualTo(1);
     *   assertThat(mock.releaseCallCount()).isEqualTo(0);
     * </pre>
     */
    class MockLockingPolicy implements LockingPolicy {

        private boolean grantLock = true;
        private int acquireCount = 0;
        private int releaseCount = 0;

        public void setGrantLock(boolean grant) {
            this.grantLock = grant;
        }

        @Override
        public boolean tryAcquire(UUID taskId, String lockHolder) {
            acquireCount++;
            return grantLock;
        }

        @Override
        public void release(UUID taskId) {
            releaseCount++;
        }

        public int acquireCallCount() { return acquireCount; }
        public int releaseCallCount() { return releaseCount; }

        public void reset() {
            acquireCount = 0;
            releaseCount = 0;
            grantLock = true;
        }
    }
}
