package com.ax.template.authblueprint.scheduledtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
 *   <li>SCHED-IDEMPOTENT-001 — JPA optimistic-lock {@code @Version} guarantees
 *       at-most-one successful UPDATE per stale takeover race.</li>
 * </ul>
 *
 * <p>Fork-receivers MAY swap this implementation for Redis Redlock, Zookeeper,
 * or Shedlock by registering a {@code @Primary} {@link LockingPolicy} bean.
 */
@Component
public class DatabaseAdvisoryLock implements LockingPolicy {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAdvisoryLock.class);

    private final TaskLockRepository repository;
    private final Clock clock;
    private final Duration ttl;

    public DatabaseAdvisoryLock(TaskLockRepository repository,
                                Clock clock,
                                ScheduledTaskProperties properties) {
        this.repository = repository;
        this.clock = clock;
        this.ttl = Duration.ofSeconds(properties.getLockTtlSeconds());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(String taskName, String lockHolder) {
        Optional<TaskLock> existing = repository.findByTaskName(taskName);
        Instant now = Instant.now(clock);

        if (existing.isEmpty()) {
            try {
                repository.saveAndFlush(new TaskLock(taskName, lockHolder, now));
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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String taskName, String lockHolder) {
        repository.findByTaskName(taskName).ifPresent(lock -> {
            if (lockHolder.equals(lock.getLockHolder())) {
                repository.delete(lock);
            }
        });
    }
}
