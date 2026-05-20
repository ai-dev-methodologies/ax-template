package com.ax.template.authblueprint.scheduledtask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IDEMPOTENCY family — SCHED-IDEMPOTENT-001 (verification_type=positive).
 * <p>
 * Spec verbatim: {@code two concurrent triggerManual() calls for the same
 * taskId. Assert taskExecutor.execute() called exactly once (one acquires
 * lock, other is skipped).}
 * <p>
 * Approach: rather than spinning real threads (race-flaky), we drive the
 * locking policy with a sequenced stub: first call returns true (winner),
 * second returns false (loser). The two triggerManual invocations exercise
 * the same code path that two real admin sessions would.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledTaskIdempotentTest {

    @Mock ScheduledTaskRepository taskRepository;
    @Mock JobHistoryRepository historyRepository;
    @Mock LockingPolicy lockingPolicy;
    @Mock TaskExecutor taskExecutor;

    ScheduledTaskService service;

    @BeforeEach
    void setUp() {
        ScheduledTaskProperties props = new ScheduledTaskProperties();
        props.setInstanceId("test-instance-A");
        service = new ScheduledTaskService(
            taskRepository, historyRepository, lockingPolicy, taskExecutor, props);
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-IDEMPOTENT-001")
    @DisplayName("SCHED-IDEMPOTENT-001 — two concurrent triggerManual ⇒ executor invoked exactly once")
    void idempotent001_concurrentTriggers_executesOnce() {
        ScheduledTask task = ScheduledTask.create("digest", "0 0 * * * ?");
        UUID id = task.getId();
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        AtomicInteger acquireCount = new AtomicInteger();
        when(lockingPolicy.tryAcquire(anyString(), anyString()))
            .thenAnswer(inv -> acquireCount.incrementAndGet() == 1); // winner=1, loser=>0

        Optional<JobHistory> first = service.triggerManual(id);
        Optional<JobHistory> second = service.triggerManual(id);

        // SCHED-IDEMPOTENT-001 — executor invoked exactly once across both triggers
        verify(taskExecutor, times(1)).execute("digest");

        assertThat(first).isPresent();
        assertThat(second)
            .as("SCHED-IDEMPOTENT-001 — second trigger observes lock held and is skipped")
            .isEmpty();

        // Only the winner writes a history row.
        verify(historyRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
        // Only the winner releases the lock (loser never acquired).
        verify(lockingPolicy, times(1)).release("digest", "test-instance-A");
    }
}
