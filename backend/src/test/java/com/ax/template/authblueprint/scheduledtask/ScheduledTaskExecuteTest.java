package com.ax.template.authblueprint.scheduledtask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EXECUTE family — SCHED-EXECUTE-001 (verification_type=both).
 * <p>
 * Spec verbatim:
 *   success: taskExecutor.execute() succeeds → JobHistory with status=SUCCESS, endTime != null.
 *   failure: taskExecutor.execute() throws → JobHistory with status=FAILED, errorMessage = exception message.
 *   lastRun on ScheduledTask is updated after successful execution only.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledTaskExecuteTest {

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
    @Tag("SCHED-EXECUTE-001")
    @DisplayName("SCHED-EXECUTE-001 success — JobHistory SUCCESS + finishedAt != null + lastRun set")
    void execute001_success() {
        ScheduledTask task = ScheduledTask.create("digest", "0 0 * * * ?");
        UUID id = task.getId();
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));
        when(lockingPolicy.tryAcquire(anyString(), anyString())).thenReturn(true);

        Optional<JobHistory> result = service.executeWithLock(id);

        assertThat(result).isPresent();
        JobHistory h = result.get();
        assertThat(h.getOutcome())
            .as("SCHED-EXECUTE-001 — success outcome")
            .isEqualTo(JobOutcome.SUCCESS);
        assertThat(h.getFinishedAt())
            .as("SCHED-EXECUTE-001 — finishedAt populated on success")
            .isNotNull();
        assertThat(h.getErrorMessage()).isNull();

        ArgumentCaptor<ScheduledTask> taskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getLastRunAt())
            .as("SCHED-EXECUTE-001 — lastRun updated on successful execution")
            .isNotNull();

        verify(historyRepository).save(any(JobHistory.class));
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-EXECUTE-001")
    @DisplayName("SCHED-EXECUTE-001 failure — JobHistory FAILED + errorMessage = exception msg + lastRun NOT updated")
    void execute001_failure() {
        ScheduledTask task = ScheduledTask.create("digest", "0 0 * * * ?");
        UUID id = task.getId();
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));
        when(lockingPolicy.tryAcquire(anyString(), anyString())).thenReturn(true);
        doThrow(new IllegalStateException("downstream timed out"))
            .when(taskExecutor).execute("digest");

        Optional<JobHistory> result = service.executeWithLock(id);

        assertThat(result).isPresent();
        JobHistory h = result.get();
        assertThat(h.getOutcome())
            .as("SCHED-EXECUTE-001 — failure outcome")
            .isEqualTo(JobOutcome.FAILED);
        assertThat(h.getFinishedAt()).isNotNull();
        assertThat(h.getErrorMessage())
            .as("SCHED-EXECUTE-001 — errorMessage captures exception message")
            .isEqualTo("downstream timed out");

        // failure path: task NOT saved (lastRun remains null)
        assertThat(task.getLastRunAt())
            .as("SCHED-EXECUTE-001 — lastRun must NOT be updated on failure")
            .isNull();

        verify(historyRepository).save(any(JobHistory.class));
        // Lock MUST still be released even on failure (try/finally contract)
        verify(lockingPolicy).release("digest", "test-instance-A");
    }
}
