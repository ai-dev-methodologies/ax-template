/**
 * FIXTURE: scheduled_task/fail_no_distributed_lock
 * Demonstrates WRONG pattern: task executes without acquiring a distributed lock.
 * In multi-node deployments, concurrent execution is not prevented (SCHED-LOCK-001).
 */
package com.example.fixture.scheduled_task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ScheduledTaskService {

    private final ScheduledTaskRepository taskRepository;
    private final TaskExecutor taskExecutor;

    public ScheduledTaskService(ScheduledTaskRepository taskRepository, TaskExecutor taskExecutor) {
        this.taskRepository = taskRepository;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void runDueTasks() {
        // BUG: no distributed lock — multiple nodes run the same task simultaneously
        taskRepository.findAllDue(Instant.now()).forEach(task -> {
            taskExecutor.execute(task);
            task.recordLastRun(Instant.now());
            taskRepository.save(task);
        });
    }
}
