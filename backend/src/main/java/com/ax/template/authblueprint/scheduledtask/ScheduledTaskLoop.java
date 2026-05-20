package com.ax.template.authblueprint.scheduledtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * In-process polling loop. Fires every {@code ax.scheduler.poll-interval-ms}
 * (default 60_000) and asks {@link ScheduledTaskService} to execute every
 * {@link ScheduledTaskStatus#ENABLED} task.
 * <p>
 * Trace:
 * <ul>
 *   <li>blueprints/scheduled-task-manifest.yaml#execute — @Scheduled fixedDelay=60_000</li>
 *   <li>SCHED-LOCK-001 — per-task {@link LockingPolicy#tryAcquire} skips when held</li>
 * </ul>
 *
 * <p>This implementation does NOT evaluate per-task cron expressions; the
 * service-level distributed lock is what guarantees at-most-one execution.
 * Fork-receivers wanting per-task cron evaluation can replace this loop with
 * Spring's {@code TaskScheduler} + {@code CronTrigger} per task.
 */
@Component
public class ScheduledTaskLoop {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskLoop.class);

    private final ScheduledTaskService service;

    public ScheduledTaskLoop(ScheduledTaskService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${ax.scheduler.poll-interval-ms:60000}",
               initialDelayString = "${ax.scheduler.initial-delay-ms:60000}")
    public void tick() {
        for (ScheduledTask task : service.listAll()) {
            if (task.getStatus() != ScheduledTaskStatus.ENABLED) {
                continue;
            }
            try {
                service.executeWithLock(task);
            } catch (RuntimeException ex) {
                log.error("scheduled-task: loop tick failed for taskName={}",
                    task.getName(), ex);
            }
        }
    }
}
