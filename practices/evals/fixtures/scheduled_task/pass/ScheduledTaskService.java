/**
 * FIXTURE: scheduled_task/pass
 * Demonstrates correct scheduled-task service:
 * - register() persists a task
 * - execute() acquires distributed lock before running
 * - duplicate execution is prevented by lock check
 * - failure is recorded in job history with retry flag
 */
package com.example.fixture.scheduled_task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ScheduledTaskService {

    private final ScheduledTaskRepository taskRepository;
    private final JobHistoryRepository historyRepository;
    private final LockingPolicy lockingPolicy;
    private final TaskExecutor taskExecutor;

    public ScheduledTaskService(
            ScheduledTaskRepository taskRepository,
            JobHistoryRepository historyRepository,
            LockingPolicy lockingPolicy,
            TaskExecutor taskExecutor) {
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.lockingPolicy = lockingPolicy;
        this.taskExecutor = taskExecutor;
    }

    @Transactional
    public ScheduledTask register(String name, String cronExpression) {
        var task = ScheduledTask.create(name, cronExpression);
        return taskRepository.save(task);
    }

    @Scheduled(fixedDelay = 60_000)
    public void runDueTasksLoop() {
        taskRepository.findAllDue(Instant.now()).forEach(this::executeWithLock);
    }

    @Transactional
    public void triggerManual(UUID taskId) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        executeWithLock(task);
    }

    private void executeWithLock(ScheduledTask task) {
        boolean acquired = lockingPolicy.tryAcquire(task.getId(), task.getName());
        if (!acquired) {
            // Duplicate run prevented by distributed lock
            return;
        }
        var history = JobHistory.start(task.getId(), task.getName());
        historyRepository.save(history);
        try {
            taskExecutor.execute(task);
            history.markSuccess(Instant.now());
            task.recordLastRun(Instant.now());
            taskRepository.save(task);
        } catch (Exception ex) {
            history.markFailure(ex.getMessage());
        } finally {
            historyRepository.save(history);
            lockingPolicy.release(task.getId());
        }
    }
}
