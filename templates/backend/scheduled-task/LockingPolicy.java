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
 *   BACKLOG P3-102 (holder-verified release): release(UUID) was an unconditional
 *   findById-then-clear — any caller could clear ANY node's lock regardless of who actually
 *   held it (lock-theft replay: a late/stale releaser deletes a lock a different node has
 *   since legitimately reclaimed). Closed by adding the `lockHolder` parameter, reading
 *   through the same findByIdForUpdate pessimistic lock tryAcquire uses, and clearing only
 *   on a holder match (mismatch = no-op + WARN, never a throw) — see DbRowLockingPolicy.release.
 *   Remaining BY-DESIGN gap (NOT closed by this fix, intentionally out of scope): this
 *   template's lock key is the scheduled_tasks row's UUID id, one row per task, whereas the
 *   production SPI (DatabaseAdvisoryLock + TaskLockRepository, backend/src) keys its
 *   task_locks table by taskName (a separate advisory-lock table, not a column pair on the
 *   domain row). The holder-VERIFICATION divergence between this template and the production
 *   SPI is now closed (both check the holder before releasing); the lock-KEY-SHAPE divergence
 *   (row-UUID vs taskName-keyed table) is a deliberate template-simplicity choice, documented
 *   here rather than silently inherited by forks.
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
import java.util.Objects;
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
 *   <li>{@link #release}: releases the lock ONLY if {@code lockHolder} is still the
 *       current holder of record; safe to call even if not held. A holder MISMATCH
 *       (the lock was reclaimed as stale and is now held by someone else, or was already
 *       released) is a no-op — logged at WARN — never a throw. An unconditional release
 *       keyed on {@code taskId} alone is a lock-theft replay: holder B still believes it
 *       holds the lock, the lock is reclaimed as stale by node C, and B's late release()
 *       then deletes C's lock state out from under it while C's job is still running —
 *       the same at-most-one violation {@link #tryAcquire} exists to prevent, from the
 *       other side. The read that decides the match MUST be the same pessimistic
 *       {@code SELECT ... FOR UPDATE} the acquire uses ({@link
 *       ScheduledTaskRepository#findByIdForUpdate}) — a plain {@code findById} lets the
 *       match-test race a concurrent stale takeover.
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
     * Releases the distributed lock for the given task, but only if {@code lockHolder}
     * is still the current holder of record. No-op (logged at WARN, never thrown) if the
     * lock is not held, or is held by a different holder — a stale/late release from a
     * holder that no longer owns the lock must never clear someone else's lock state.
     *
     * @param taskId     UUID of the task
     * @param lockHolder string identifying the node that believes it holds the lock
     */
    void release(UUID taskId, String lockHolder);

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
     *
     * <p>Release: also reads through {@code SELECT ... FOR UPDATE} and clears the lock
     * ONLY if the caller's {@code lockHolder} still matches {@code task.getLockHolder()}.
     * Without the row lock and the match test, a holder whose lock was already reclaimed
     * as stale by another node could delete that other node's brand-new lock — the same
     * at-most-one guarantee {@code tryAcquire} provides, broken from the release side.
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
        public void release(UUID taskId, String lockHolder) {
            // FOR UPDATE, not findById: the holder-match test and the clearing write below
            // must be one indivisible step, or a concurrent stale takeover can commit between
            // the test and the write and have its brand-new lock deleted by this stale caller
            // (lock-theft replay — the same hazard tryAcquire's row lock prevents).
            taskRepository.findByIdForUpdate(taskId).ifPresent(task -> {
                if (!Objects.equals(lockHolder, task.getLockHolder())) {
                    log.warn("Release attempted by non-holder (expected={}, actual={}) — "
                            + "no-op for taskId={}", task.getLockHolder(), lockHolder, taskId);
                    return;
                }
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
     * Tracks which holder was actually granted the lock and, analogous to
     * {@link DbRowLockingPolicy#release}, only counts a {@link #release} as real when the
     * releasing holder matches — a mismatched holder is a no-op, never a throw, so this
     * double does not silently diverge from the production holder-verified contract.
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
        private String grantedHolder;

        public void setGrantLock(boolean grant) {
            this.grantLock = grant;
        }

        @Override
        public boolean tryAcquire(UUID taskId, String lockHolder) {
            acquireCount++;
            if (grantLock) {
                grantedHolder = lockHolder;
            }
            return grantLock;
        }

        @Override
        public void release(UUID taskId, String lockHolder) {
            if (grantedHolder != null && grantedHolder.equals(lockHolder)) {
                releaseCount++;
                grantedHolder = null;
            }
        }

        public int acquireCallCount() { return acquireCount; }
        public int releaseCallCount() { return releaseCount; }

        public void reset() {
            acquireCount = 0;
            releaseCount = 0;
            grantLock = true;
            grantedHolder = null;
        }
    }
}
