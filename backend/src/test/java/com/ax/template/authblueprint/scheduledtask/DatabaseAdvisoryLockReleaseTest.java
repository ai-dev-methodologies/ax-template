package com.ax.template.authblueprint.scheduledtask;

import com.ax.template.authblueprint.common.IdempotentInsert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LOCK family — {@link DatabaseAdvisoryLock#release} holder-verification coverage
 * (BACKLOG P3-102).
 * <p>
 * {@code release(taskName, lockHolder)} was already holder-verified in this backend when this
 * test was authored — it clears a lock only if {@code lockHolder} still matches the row's
 * current holder, and reads that row through {@link TaskLockRepository#findByTaskNameForUpdate}
 * (the same pessimistic {@code SELECT ... FOR UPDATE} query {@code tryAcquire} uses). What was
 * MISSING was direct test coverage of {@code DatabaseAdvisoryLock} itself: every existing
 * release-path test in this suite ({@link ScheduledTaskLockTest},
 * {@link ScheduledTaskExecuteTest}, {@link ScheduledTaskIdempotentTest}) mocks
 * {@link LockingPolicy} wholesale, so they prove {@code ScheduledTaskService} CALLS
 * {@code release(taskName, holder)} with the right arguments but never exercise
 * {@code DatabaseAdvisoryLock}'s own release logic — exactly the surface a cross-family review
 * flagged as a P1 in the fork-template twin of this class
 * ({@code templates/backend/scheduled-task/LockingPolicy.java}: an unconditional, non-locking
 * {@code release(UUID)} is a lock-theft replay — holder B still believes it holds the lock, node
 * C reclaims it as stale, and B's late release deletes C's lock state out from under it while
 * C's job is still running).
 * <p>
 * Three behaviors, asserted against the repository collaborator rather than by prose:
 * <ul>
 *   <li>{@link #releaseWithWrongHolder_isNoOp()} — a holder mismatch never deletes the row and
 *       never throws.</li>
 *   <li>{@link #releaseWithMatchingHolder_clearsLock()} — a holder match deletes the row.</li>
 *   <li>{@link #release_readsThroughThePessimisticForUpdateQuery()} — the release path reads via
 *       {@code findByTaskNameForUpdate}, never the plain derived {@code findById} — the query
 *       {@link TaskLockRepository}'s own javadoc names as "the ONLY read of a lock row on the
 *       acquire/release paths".</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DatabaseAdvisoryLockReleaseTest {

    private static final String TASK_NAME = "digest-job";

    @Mock TaskLockRepository repository;
    @Mock IdempotentInsert idempotentInsert;
    @Mock Clock clock;

    private DatabaseAdvisoryLock lockingPolicy;

    @BeforeEach
    void setUp() {
        // release() touches neither idempotentInsert nor clock nor the TTL derived from
        // properties — a plain instance matches this suite's convention (ScheduledTaskLockTest)
        // of instantiating the real config POJO rather than mocking it.
        ScheduledTaskProperties properties = new ScheduledTaskProperties();
        lockingPolicy = new DatabaseAdvisoryLock(repository, idempotentInsert, clock, properties);
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-LOCK-001")
    @DisplayName("SCHED-LOCK-001 — release() with a non-matching holder is a no-op, never throws")
    void releaseWithWrongHolder_isNoOp() {
        TaskLock lock = new TaskLock(TASK_NAME, "holder-A", Instant.now());
        when(repository.findByTaskNameForUpdate(TASK_NAME)).thenReturn(Optional.of(lock));

        lockingPolicy.release(TASK_NAME, "holder-B");

        verify(repository, never()).delete(any(TaskLock.class));
        // the row itself is untouched — release() never mutates a lock it does not own
        assertThat(lock.getLockHolder()).isEqualTo("holder-A");
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-LOCK-001")
    @DisplayName("SCHED-LOCK-001 — release() with the matching holder clears the lock row")
    void releaseWithMatchingHolder_clearsLock() {
        TaskLock lock = new TaskLock(TASK_NAME, "holder-A", Instant.now());
        when(repository.findByTaskNameForUpdate(TASK_NAME)).thenReturn(Optional.of(lock));

        lockingPolicy.release(TASK_NAME, "holder-A");

        verify(repository).delete(lock);
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-LOCK-001")
    @DisplayName("SCHED-LOCK-001 — release() reads the lock row through the pessimistic "
            + "FOR UPDATE query, never a plain findById")
    void release_readsThroughThePessimisticForUpdateQuery() {
        when(repository.findByTaskNameForUpdate(TASK_NAME)).thenReturn(Optional.empty());

        lockingPolicy.release(TASK_NAME, "holder-A");

        verify(repository).findByTaskNameForUpdate(TASK_NAME);
        verify(repository, never()).findById(anyString());
    }
}
