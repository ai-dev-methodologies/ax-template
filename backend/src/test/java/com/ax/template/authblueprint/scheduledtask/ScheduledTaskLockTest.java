package com.ax.template.authblueprint.scheduledtask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LOCK family — SCHED-LOCK-001, SCHED-LOCK-002.
 * <p>
 * SCHED-LOCK-001 (verification_type=both):
 *   positive — tryAcquire() returns true → taskExecutor.execute() called once.
 *   negative — tryAcquire() returns false → taskExecutor.execute() NEVER called.
 * <p>
 * SCHED-LOCK-002 (verification_type=positive):
 *   stale lock row (lockedAt > TTL ago) → tryAcquire() returns true and the
 *   row is overwritten with the new holder.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledTaskLockTest {

    @Mock ScheduledTaskRepository taskRepository;
    @Mock JobHistoryRepository historyRepository;
    @Mock LockingPolicy lockingPolicy;
    @Mock TaskExecutor taskExecutor;

    ScheduledTaskService service;
    ScheduledTaskProperties props;

    @BeforeEach
    void setUp() {
        props = new ScheduledTaskProperties();
        props.setInstanceId("test-instance-A");
        service = new ScheduledTaskService(
            taskRepository, historyRepository, lockingPolicy, taskExecutor, props);
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-LOCK-001")
    @DisplayName("SCHED-LOCK-001 positive — tryAcquire=true ⇒ executor invoked exactly once + SUCCESS history written")
    void lock001_positive_executesAndRecords() {
        ScheduledTask task = ScheduledTask.create("digest-job", "0 0 * * * ?");
        UUID id = task.getId();
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));
        when(lockingPolicy.tryAcquire("digest-job", "test-instance-A")).thenReturn(true);

        Optional<JobHistory> result = service.executeWithLock(id);

        verify(taskExecutor, times(1)).execute("digest-job");
        verify(lockingPolicy).release("digest-job", "test-instance-A");
        assertThat(result).isPresent();
        assertThat(result.get().getOutcome()).isEqualTo(JobOutcome.SUCCESS);
        verify(historyRepository).save(any(JobHistory.class));
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-LOCK-001")
    @DisplayName("SCHED-LOCK-001 negative — tryAcquire=false ⇒ executor NEVER called, no history row")
    void lock001_negative_skipsExecution() {
        ScheduledTask task = ScheduledTask.create("digest-job", "0 0 * * * ?");
        UUID id = task.getId();
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));
        when(lockingPolicy.tryAcquire("digest-job", "test-instance-A")).thenReturn(false);

        Optional<JobHistory> result = service.executeWithLock(id);

        verify(taskExecutor, never()).execute(anyString());
        verify(historyRepository, never()).save(any(JobHistory.class));
        verify(lockingPolicy, never()).release(anyString(), anyString());
        assertThat(result).isEmpty();
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-LOCK-002")
    @DisplayName("SCHED-LOCK-002 — stale lock (locked_at + TTL < now) is reclaimed and lock row updated")
    void lock002_staleLock_isReclaimedByNewHolder() {
        // Build a real DatabaseAdvisoryLock with a fake Clock + TaskLockRepository
        // to drive the SCHED-LOCK-002 stale-reclaim branch deterministically.
        Instant t0 = Instant.parse("2026-05-20T10:00:00Z");
        Instant tNow = t0.plus(Duration.ofSeconds(301)); // 1 second past TTL=300
        Clock fixedNow = Clock.fixed(tNow, ZoneOffset.UTC);

        TaskLockRepository lockRepo = org.mockito.Mockito.mock(TaskLockRepository.class);
        ScheduledTaskProperties properties = new ScheduledTaskProperties();
        properties.setLockTtlSeconds(300);

        TaskLock staleLock = new TaskLock("digest-job", "previous-instance-Z", t0);
        when(lockRepo.findByTaskNameForUpdate("digest-job")).thenReturn(Optional.of(staleLock));

        DatabaseAdvisoryLock dbLock = new DatabaseAdvisoryLock(
            lockRepo, new com.ax.template.authblueprint.common.IdempotentInsert(), fixedNow, properties);

        boolean acquired = dbLock.tryAcquire("digest-job", "new-instance-A");

        assertThat(acquired)
            .as("SCHED-LOCK-002 — stale lock past TTL must be re-acquirable")
            .isTrue();

        ArgumentCaptor<TaskLock> captor = ArgumentCaptor.forClass(TaskLock.class);
        verify(lockRepo).saveAndFlush(captor.capture());
        TaskLock updated = captor.getValue();
        assertThat(updated.getTaskName()).isEqualTo("digest-job");
        assertThat(updated.getLockHolder())
            .as("SCHED-LOCK-002 — lock row updated with new holder")
            .isEqualTo("new-instance-A");
        assertThat(updated.getLockedAt()).isEqualTo(tNow);
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-LOCK-002")
    @DisplayName("SCHED-LOCK-002 sanity — fresh lock (within TTL) is NOT reclaimable")
    void lock002_freshLock_notReclaimable() {
        Instant t0 = Instant.parse("2026-05-20T10:00:00Z");
        Instant tNow = t0.plus(Duration.ofSeconds(100)); // well within TTL=300
        Clock fixedNow = Clock.fixed(tNow, ZoneOffset.UTC);

        TaskLockRepository lockRepo = org.mockito.Mockito.mock(TaskLockRepository.class);
        ScheduledTaskProperties properties = new ScheduledTaskProperties();
        properties.setLockTtlSeconds(300);

        TaskLock freshLock = new TaskLock("digest-job", "previous-instance-Z", t0);
        when(lockRepo.findByTaskNameForUpdate("digest-job")).thenReturn(Optional.of(freshLock));

        DatabaseAdvisoryLock dbLock = new DatabaseAdvisoryLock(
            lockRepo, new com.ax.template.authblueprint.common.IdempotentInsert(), fixedNow, properties);

        boolean acquired = dbLock.tryAcquire("digest-job", "new-instance-A");

        assertThat(acquired)
            .as("Fresh lock within TTL must not be reclaimed")
            .isFalse();
        verify(lockRepo, never()).saveAndFlush(any(TaskLock.class));
    }
}
