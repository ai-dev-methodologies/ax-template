package com.ax.template.authblueprint.scheduledtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain orchestrator for the scheduled-task surface.
 * <p>
 * Trace:
 * <ul>
 *   <li>SCHED-REGISTER-001 — {@link #register(String, String)} persists with
 *       status=REGISTERED and returns a saved entity with a generated UUID.</li>
 *   <li>SCHED-LOCK-001 — {@link #executeWithLock(UUID)} consults
 *       {@link LockingPolicy#tryAcquire} and SKIPS execution when the lock is
 *       held by another holder.</li>
 *   <li>SCHED-EXECUTE-001 — every execution (success or failure) writes a
 *       {@link JobHistory} row; failures capture the exception message.</li>
 *   <li>SCHED-IDEMPOTENT-001 — {@link #triggerManual(UUID)} delegates to
 *       {@code executeWithLock}, so two concurrent admin triggers result in
 *       exactly one execution (the other observes lock held and is skipped).</li>
 * </ul>
 */
@Service
public class ScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final ScheduledTaskRepository taskRepository;
    private final JobHistoryRepository historyRepository;
    private final LockingPolicy lockingPolicy;
    private final TaskExecutor taskExecutor;
    private final ScheduledTaskProperties properties;

    public ScheduledTaskService(ScheduledTaskRepository taskRepository,
                                JobHistoryRepository historyRepository,
                                LockingPolicy lockingPolicy,
                                TaskExecutor taskExecutor,
                                ScheduledTaskProperties properties) {
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.lockingPolicy = lockingPolicy;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
    }

    /**
     * SCHED-REGISTER-001 — persists a new task. Cron parsing is advisory
     * (manifest #register.cron_validation=advisory): an invalid expression logs
     * a WARN and is still stored, so the admin can correct it later.
     */
    @Transactional
    public ScheduledTask register(String name, String cronExpression) {
        validateCronAdvisory(cronExpression);
        ScheduledTask task = ScheduledTask.create(name, cronExpression);
        return taskRepository.save(task);
    }

    @Transactional
    public ScheduledTask register(String name, String cronExpression, String handlerBean) {
        validateCronAdvisory(cronExpression);
        ScheduledTask task = ScheduledTask.create(name, cronExpression, handlerBean);
        return taskRepository.save(task);
    }

    private void validateCronAdvisory(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
        } catch (RuntimeException ex) {
            log.warn("scheduled-task: invalid cron expression accepted (advisory) cron={} error={}",
                cronExpression, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<ScheduledTask> findById(UUID id) {
        return taskRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTask> listAll() {
        return taskRepository.findAll();
    }

    @Transactional
    public ScheduledTask enable(UUID id) {
        ScheduledTask task = taskRepository.findById(id)
            .orElseThrow(() -> new ScheduledTaskNotFoundException(id));
        task.enable();
        return taskRepository.save(task);
    }

    @Transactional
    public ScheduledTask disable(UUID id) {
        ScheduledTask task = taskRepository.findById(id)
            .orElseThrow(() -> new ScheduledTaskNotFoundException(id));
        task.disable();
        return taskRepository.save(task);
    }

    /**
     * SCHED-EXECUTE-001 + SCHED-LOCK-001 — try to acquire the lock, run the
     * executor, and record one {@link JobHistory} row regardless of outcome.
     *
     * @return the persisted history row, or {@link Optional#empty()} when the
     *     lock was already held (skip, no history row written).
     */
    public Optional<JobHistory> executeWithLock(UUID taskId) {
        ScheduledTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ScheduledTaskNotFoundException(taskId));
        return executeWithLock(task);
    }

    /** Overload used by the in-process scheduler loop that already has the entity. */
    public Optional<JobHistory> executeWithLock(ScheduledTask task) {
        String holder = properties.getInstanceId();
        boolean acquired = lockingPolicy.tryAcquire(task.getName(), holder);
        if (!acquired) {
            log.debug("scheduled-task: lock held, skipping taskName={}", task.getName());
            return Optional.empty();
        }

        JobHistory history = JobHistory.start(task.getId(), task.getName(), holder);
        Instant runStart = Instant.now();
        try {
            taskExecutor.execute(task.getName());
            history.markSuccess();
            task.markLastRun(runStart);
            taskRepository.save(task);
            log.info("scheduled-task: SUCCESS taskName={} durationMs={}",
                task.getName(), Duration.between(runStart, Instant.now()).toMillis());
        } catch (RuntimeException ex) {
            String raw = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            // R63 — anchor R61 server-side-stored-error-sanitize. The
            // job-history.error_message column is read by admins via the
            // scheduled-task admin UI and by SREs via direct SQL. Scrub
            // PII (RRN / mobile / JWT / Bearer / email / internal hosts)
            // at storage time so neither path reads raw values.
            String msg = com.ax.template.authblueprint.emailoutbox.EmailPiiHelper.sanitizeReason(raw);
            history.markFailure(msg);
            log.error("scheduled-task: FAILED taskName={} error={}", task.getName(), msg, ex);
        } finally {
            historyRepository.save(history);
            lockingPolicy.release(task.getName(), holder);
        }
        return Optional.of(history);
    }

    /**
     * SCHED-IDEMPOTENT-001 — manual admin trigger. Two concurrent triggerManual
     * calls for the same taskId produce exactly one executed run; the loser
     * observes the lock as held and is skipped.
     */
    public Optional<JobHistory> triggerManual(UUID taskId) {
        return executeWithLock(taskId);
    }
}
