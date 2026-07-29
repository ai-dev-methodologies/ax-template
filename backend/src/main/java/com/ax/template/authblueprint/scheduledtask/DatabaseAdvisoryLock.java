package com.ax.template.authblueprint.scheduledtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ax.template.authblueprint.common.IdempotentInsert;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Default {@link LockingPolicy} backed by a database row in {@code task_locks}.
 * <p>
 * Trace:
 * <ul>
 *   <li>SCHED-LOCK-001 — INSERT-or-no-op acquire: if the row exists and is not
 *       stale, return false (skip execution).</li>
 *   <li>SCHED-LOCK-002 — stale-lock takeover: if {@code lockedAt + ttl < now},
 *       UPDATE the row with the new holder and return true.</li>
 *   <li>SCHED-IDEMPOTENT-001 — at-most-one acquirer per TTL window; see the
 *       mechanism below.</li>
 * </ul>
 *
 * <h2>What makes the acquire atomic (BACKLOG P2-48)</h2>
 * Two DIFFERENT primitives cover the two branches, because the row a claimant
 * must beat does not exist in the first one:
 * <ul>
 *   <li><b>First acquire (row absent)</b> — the {@code task_locks.task_name}
 *       PRIMARY KEY is the arbiter: every racer INSERTs, the database admits
 *       exactly one, and the losers' {@link DataIntegrityViolationException} is
 *       caught and reported as "lock held" (false). No read participates in the
 *       decision, so there is no window to lose. Two details make that hold and
 *       neither is optional: {@link TaskLock} declares itself {@code Persistable}
 *       so the write is a {@code persist} (a real INSERT the PK can reject) and not
 *       a {@code merge} (a SELECT-then-UPDATE that would overwrite the winner), and
 *       the insert runs behind {@link IdempotentInsert} so the rejection rolls back
 *       only its own transaction.</li>
 *   <li><b>Stale takeover (row present)</b> — {@link
 *       TaskLockRepository#findByTaskNameForUpdate} is a pessimistic
 *       {@code SELECT ... FOR UPDATE}, so the staleness test and the takeover
 *       UPDATE happen while this transaction HOLDS the row lock. A racer blocks
 *       on that lock, and when it is granted the racer re-reads the winner's
 *       committed {@code lockedAt} — no longer stale — and returns false.</li>
 * </ul>
 * The whole sequence runs in a {@code REQUIRES_NEW} transaction, so the row lock
 * is held for the acquire only and never for the duration of the job.
 * <p>
 * Deliberately NOT {@code SKIP LOCKED}: an empty result set from a skipped row
 * is indistinguishable from "row absent", which would send a loser down the
 * INSERT branch on every contended acquire. (It is also unsupported by H2, which
 * the reference workload runs on.) {@code TaskLock}'s {@code @Version} remains a
 * second-line backstop for any future non-locking read path, but it is NOT what
 * makes this method correct — an earlier revision of this javadoc claimed it was,
 * while the acquire was in fact a read-then-write with both racers able to pass.
 *
 * <p>Fork-receivers MAY swap this implementation for Redis Redlock, Zookeeper,
 * or Shedlock by registering a {@code @Primary} {@link LockingPolicy} bean.
 */
@Component
public class DatabaseAdvisoryLock implements LockingPolicy {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAdvisoryLock.class);

    private final TaskLockRepository repository;
    private final IdempotentInsert idempotentInsert;
    private final Clock clock;
    private final Duration ttl;

    public DatabaseAdvisoryLock(TaskLockRepository repository,
                                IdempotentInsert idempotentInsert,
                                Clock clock,
                                ScheduledTaskProperties properties) {
        this.repository = repository;
        this.idempotentInsert = idempotentInsert;
        this.clock = clock;
        this.ttl = Duration.ofSeconds(properties.getLockTtlSeconds());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(String taskName, String lockHolder) {
        // SELECT ... FOR UPDATE — holds the row lock for the staleness-test/takeover pair below.
        Optional<TaskLock> existing = repository.findByTaskNameForUpdate(taskName);
        Instant now = Instant.now(clock);

        if (existing.isEmpty()) {
            try {
                // The insert crosses the IdempotentInsert bean boundary so the duplicate-key
                // failure rolls back ONLY that inner REQUIRES_NEW transaction. Flushing it
                // inline instead would mark THIS transaction rollback-only, and the `false`
                // returned below would be overwritten at commit by an UnexpectedRollbackException
                // — the loser would throw rather than report a lost race (P2-48). Safe as a
                // TERMINAL insert: nothing is written afterwards and no row lock is held (the
                // FOR UPDATE above matched no row).
                idempotentInsert.insert(
                    () -> repository.saveAndFlush(new TaskLock(taskName, lockHolder, now)));
                return true;
            } catch (DataIntegrityViolationException dup) {
                // Concurrent insert race — another node won.
                log.debug("scheduled-task: concurrent insert raced for {} → skip", taskName);
                return false;
            }
        }

        TaskLock lock = existing.get();
        Instant stalenessCutoff = lock.getLockedAt().plus(ttl);
        if (now.isBefore(stalenessCutoff)) {
            // Active lock held by another holder.
            return false;
        }

        // Stale lock takeover — SCHED-LOCK-002.
        log.warn("scheduled-task: stale lock reclaim taskName={} previousHolder={} ageSeconds={}",
            taskName, lock.getLockHolder(),
            Duration.between(lock.getLockedAt(), now).toSeconds());
        lock.takeOver(lockHolder, now);
        repository.saveAndFlush(lock);
        return true;
    }

    /**
     * Releases only a lock this holder still owns. The read is the same
     * {@code SELECT ... FOR UPDATE} the acquire uses (P2-48): without the row lock, a
     * stale takeover committing between the ownership test and the DELETE would let a
     * departing holder delete the NEW holder's lock — dropping the guard while that
     * holder's job is still running, which is the same at-most-one violation from the
     * other side.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String taskName, String lockHolder) {
        repository.findByTaskNameForUpdate(taskName).ifPresent(lock -> {
            if (lockHolder.equals(lock.getLockHolder())) {
                repository.delete(lock);
            }
        });
    }
}
