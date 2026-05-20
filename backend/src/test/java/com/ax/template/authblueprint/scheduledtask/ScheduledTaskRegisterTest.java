package com.ax.template.authblueprint.scheduledtask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REGISTER family — SCHED-REGISTER-001.
 * <p>
 * Spec verbatim: {@code register("cleanup-expired-tokens", "0 0 2 * * ?") →
 * assert saved ScheduledTask has status=REGISTERED, id != null.}
 */
@ExtendWith(MockitoExtension.class)
class ScheduledTaskRegisterTest {

    @Mock ScheduledTaskRepository taskRepository;
    @Mock JobHistoryRepository historyRepository;
    @Mock LockingPolicy lockingPolicy;
    @Mock TaskExecutor taskExecutor;

    ScheduledTaskService service;

    @BeforeEach
    void setUp() {
        ScheduledTaskProperties props = new ScheduledTaskProperties();
        service = new ScheduledTaskService(
            taskRepository, historyRepository, lockingPolicy, taskExecutor, props);
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-REGISTER-001")
    @DisplayName("SCHED-REGISTER-001 — register() persists a ScheduledTask with status=REGISTERED and a generated UUID")
    void register001_persists_with_status_and_uuid() {
        when(taskRepository.save(any(ScheduledTask.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ScheduledTask saved = service.register("cleanup-expired-tokens", "0 0 2 * * ?");

        ArgumentCaptor<ScheduledTask> captor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(taskRepository).save(captor.capture());
        ScheduledTask persisted = captor.getValue();

        assertThat(persisted.getId())
            .as("SCHED-REGISTER-001 — generated UUID")
            .isNotNull();
        assertThat(persisted.getName()).isEqualTo("cleanup-expired-tokens");
        assertThat(persisted.getCronExpression()).isEqualTo("0 0 2 * * ?");
        assertThat(persisted.getStatus())
            .as("SCHED-REGISTER-001 — initial status MUST be REGISTERED")
            .isEqualTo(ScheduledTaskStatus.REGISTERED);

        assertThat(saved.getId()).isInstanceOf(UUID.class);
        assertThat(saved.getStatus()).isEqualTo(ScheduledTaskStatus.REGISTERED);
    }

    @Test
    @Tag("SCHEDULED_TASK")
    @Tag("SCHED-REGISTER-001")
    @DisplayName("SCHED-REGISTER-001 — invalid cron is advisory: still persisted (manifest cron_validation=advisory)")
    void register001_invalidCron_isAdvisory_stillPersisted() {
        when(taskRepository.save(any(ScheduledTask.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Wildly invalid expression → advisory WARN, NOT a hard fail
        ScheduledTask saved = service.register("bad-cron-task", "not a cron expression at all");

        assertThat(saved.getCronExpression()).isEqualTo("not a cron expression at all");
        assertThat(saved.getStatus()).isEqualTo(ScheduledTaskStatus.REGISTERED);
        verify(taskRepository).save(any(ScheduledTask.class));
    }
}
