/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/ScheduledTaskService
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule: async-scheduled-fixed-delay-vs-fixed-rate.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Scheduled annotation for polling loops"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
 *   - source_type: external
 *     citation: "ShedLock — distributed lock for Spring scheduled tasks"
 *     url: "https://github.com/lukas-krecan/ShedLock"
 *   - source_type: external
 *     citation: "OWASP ASVS V4 — Verify business logic limits prevent abuse"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   ScheduledTaskService owns all business logic for the scheduled-task domain:
 *     - register(): persist a new task
 *     - runDueTasksLoop(): @Scheduled loop — acquires lock, executes, records history
 *     - triggerManual(): admin-initiated run (same lock path)
 *     - list/get: admin queries
 *   Extends BaseService (SP13).
 */
package com.example.app.scheduledtask;

import com.example.app.common.BaseService;
import org.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Business logic for the scheduled-task domain.
 *
 * <p>Execution flow per task:
 * <ol>
 *   <li>Acquire distributed lock via {@link LockingPolicy#tryAcquire}
 *   <li>If lock not acquired → skip (concurrent node running same task)
 *   <li>Create {@link JobHistory} with status RUNNING
 *   <li>Execute via {@link TaskExecutor}
 *   <li>Update history (SUCCESS or FAILED) and record lastRunAt in finally block
 *   <li>Release lock in finally block
 * </ol>
 *
 * <p>Extends {@link BaseService} (SP13) for shared exception helpers.
 */
@Service
@Transactional(readOnly = true)
public class ScheduledTaskService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final ScheduledTaskRepository taskRepository;
    private final JobHistoryRepository historyRepository;
    private final LockingPolicy lockingPolicy;
    private final TaskExecutor taskExecutor;
    private final String lockHolderNodeId;
    private final int historyRetentionDays;

    public ScheduledTaskService(
            ScheduledTaskRepository taskRepository,
            JobHistoryRepository historyRepository,
            LockingPolicy lockingPolicy,
            TaskExecutor taskExecutor,
            @Value("${spring.application.name:ax-template}") String appName,
            @Value("${ax.scheduler.history-retention-days:90}") int historyRetentionDays) {
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.lockingPolicy = lockingPolicy;
        this.taskExecutor = taskExecutor;
        // Node ID: appName + random suffix (replace with hostname:pid in production)
        this.lockHolderNodeId = appName + "@" + UUID.randomUUID().toString().substring(0, 8);
        this.historyRetentionDays = historyRetentionDays;
    }

    // ─── register ─────────────────────────────────────────────────────────

    /**
     * Registers a new scheduled task.
     *
     * @param name           unique task name (e.g., "cleanup-expired-tokens")
     * @param cronExpression cron expression (advisory validation — log WARN on parse error)
     * @return persisted ScheduledTask with status REGISTERED
     */
    @Transactional
    public ScheduledTask register(String name, String cronExpression) {
        var task = ScheduledTask.create(name, cronExpression);
        log.info("Registering scheduled task '{}' cron='{}'", name, cronExpression);
        return taskRepository.save(task);
    }

    // ─── scheduled loop ───────────────────────────────────────────────────

    /**
     * Polls for eligible tasks and runs them with distributed lock protection.
     * Runs every 60 seconds (configurable via ax.scheduler.poll-interval-ms).
     */
    @Scheduled(fixedDelayString = "${ax.scheduler.poll-interval-ms:60000}")
    @Transactional
    public void runDueTasksLoop() {
        var eligible = taskRepository.findAllEligible();
        if (!eligible.isEmpty()) {
            log.debug("ScheduledTask loop: {} eligible tasks", eligible.size());
        }
        eligible.forEach(this::executeWithLock);
    }

    // ─── admin trigger ────────────────────────────────────────────────────

    /**
     * Manually triggers a task run from the admin API.
     *
     * <p>Uses the same lock path as the scheduled loop — concurrent admin triggers
     * are idempotent (at most one execution per lock TTL, SCHED-IDEMPOTENT-001).
     *
     * @param taskId UUID of the task to trigger
     * @return true if execution started; false if lock was held (already running)
     */
    @Transactional
    public boolean triggerManual(UUID taskId) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> entityNotFound("ScheduledTask", taskId));
        return executeWithLock(task);
    }

    // ─── admin queries ────────────────────────────────────────────────────

    /**
     * Paginated list of tasks for admin view.
     */
    public Page<ScheduledTask> listForAdmin(
            ScheduledTask.ScheduledTaskStatus statusFilter, Pageable pageable) {
        return taskRepository.findAllForAdmin(statusFilter, pageable);
    }

    /**
     * Single task for admin view.
     */
    public ScheduledTask getForAdmin(UUID id) {
        return taskRepository.findActiveById(id)
                .orElseThrow(() -> entityNotFound("ScheduledTask", id));
    }

    /**
     * Paginated job history for a specific task.
     */
    public Page<JobHistory> getHistory(UUID taskId, Pageable pageable) {
        // Verify task exists (throws 404 if not)
        getForAdmin(taskId);
        return historyRepository.findByTaskId(taskId, pageable);
    }

    // ─── retention cleanup ────────────────────────────────────────────────

    /**
     * Prunes JobHistory records older than {@code historyRetentionDays} days.
     * Intended to be called by a registered task handler, e.g.:
     * <pre>
     *   executor.register("prune-job-history", () -> scheduledTaskService.pruneHistory());
     * </pre>
     */
    @Transactional
    public int pruneHistory() {
        var cutoff = Instant.now().minus(historyRetentionDays, ChronoUnit.DAYS);
        int deleted = historyRepository.pruneOlderThan(cutoff);
        log.info("Pruned {} JobHistory records older than {} days", deleted, historyRetentionDays);
        return deleted;
    }

    // ─── internal ─────────────────────────────────────────────────────────

    /**
     * Acquires distributed lock, runs the task, records history, and releases lock.
     *
     * @return true if execution ran; false if lock was not acquired (skip)
     */
    private boolean executeWithLock(ScheduledTask task) {
        boolean acquired = lockingPolicy.tryAcquire(task.getId(), lockHolderNodeId);
        if (!acquired) {
            log.debug("Lock held — skipping task '{}' (id={})", task.getName(), task.getId());
            return false;
        }

        var history = JobHistory.start(task.getId(), task.getName());
        historyRepository.save(history);

        try {
            log.info("Executing task '{}' (id={})", task.getName(), task.getId());
            taskExecutor.execute(task);
            history.markSuccess(Instant.now());
            task.recordLastRun(Instant.now());
            taskRepository.save(task);
            log.info("Task '{}' completed successfully", task.getName());
            return true;
        } catch (Exception ex) {
            history.markFailure(ex.getMessage());
            log.error("Task '{}' failed: {}", task.getName(), ex.getMessage(), ex);
            return false;
        } finally {
            historyRepository.save(history);
            lockingPolicy.release(task.getId());
        }
    }
}
