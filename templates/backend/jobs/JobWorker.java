/**
 * @ax-template-meta
 * template_id: backend/jobs/JobWorker
 * layer: backend-application
 * domain: jobs
 * anchors_rule: async-scheduled-fixed-delay-vs-fixed-rate.md (PRACTICES-ASYNC-002)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Scheduled(fixedDelay): next invocation waits fixedDelay ms after the previous execution completes; prevents overlap under slow execution unlike fixedRate"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled"
 *   - source_type: external
 *     citation: "ShedLock GitHub — Distributed lock for scheduled tasks; prevents concurrent execution across multiple application instances without a message broker"
 *     url: "https://github.com/lukas-krecan/ShedLock"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Register JobHandler implementations as Spring beans with their jobType string.
 *   JobWorker discovers all JobHandler beans at startup and dispatches by jobType.
 *   For multi-instance deployments, add ShedLock (@SchedulerLock) to processQueue()
 *   to prevent concurrent processing across nodes.
 *   Configure job.worker.batch-size and job.worker.fixed-delay-ms in application.properties.
 */
package com.example.app.jobs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background worker that polls the {@code job_queue} table and executes pending jobs.
 *
 * <h3>Execution model</h3>
 * <ol>
 *   <li>Poll: load up to {@code batchSize} PENDING rows ordered by {@code created_at}.</li>
 *   <li>Lock: acquire a pessimistic write lock on each row to prevent concurrent execution.</li>
 *   <li>Execute: resolve the matching {@link JobHandler} by {@code job_type} and invoke it.</li>
 *   <li>Update: transition to DONE on success; FAILED on exception (error message stored).</li>
 * </ol>
 *
 * <h3>At-least-once delivery</h3>
 * If the process crashes after execution but before the DONE update is committed, the
 * job remains RUNNING and will be picked up again on restart (or by a separate stale-job
 * recovery scheduled task). All job handlers must be idempotent.
 *
 * <h3>Multi-instance</h3>
 * For multi-node deployments add {@code @SchedulerLock(name = "jobWorkerLock")} (ShedLock)
 * to {@link #processQueue()} to prevent concurrent execution across nodes.
 */
@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    @PersistenceContext
    private EntityManager em;

    private final Map<String, JobHandler> handlersByType;
    private final int batchSize;

    public JobWorker(
            List<JobHandler> handlers,
            @Value("${job.worker.batch-size:10}") int batchSize) {
        this.handlersByType = handlers.stream()
                .collect(Collectors.toMap(JobHandler::jobType, Function.identity()));
        this.batchSize = batchSize;
    }

    /**
     * Polls the job queue and processes up to {@code batchSize} pending jobs.
     *
     * <p>Runs every {@code job.worker.fixed-delay-ms} milliseconds (default: 5 000 ms).
     * Uses {@code fixedDelay} (not {@code fixedRate}) so the next poll waits until the
     * previous batch completes — preventing queue saturation under slow job handlers.
     */
    @Scheduled(fixedDelayString = "${job.worker.fixed-delay-ms:5000}")
    @Transactional
    public void processQueue() {
        @SuppressWarnings("unchecked")
        List<JobQueue> pending = em.createQuery(
                "SELECT j FROM JobQueue j WHERE j.status = :status ORDER BY j.createdAt ASC",
                JobQueue.class)
                .setParameter("status", JobQueue.JobStatus.PENDING)
                .setMaxResults(batchSize)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        for (JobQueue job : pending) {
            processJob(job);
        }
    }

    private void processJob(JobQueue job) {
        job.markRunning();

        JobHandler handler = handlersByType.get(job.getJobType());
        if (handler == null) {
            job.markFailed("No handler registered for job type: " + job.getJobType());
            log.error("No handler for job type '{}' (job id={})", job.getJobType(), job.getId());
            return;
        }

        try {
            handler.execute(job.getJobType(), job.getPayload());
            job.markDone();
            log.debug("Job completed: type={} id={}", job.getJobType(), job.getId());
        } catch (Exception ex) {
            job.markFailed(ex.getMessage());
            log.error("Job failed: type={} id={} error={}", job.getJobType(), job.getId(), ex.getMessage(), ex);
        }
    }

    /**
     * Handler SPI — implement and register as a Spring bean to handle a job type.
     */
    public interface JobHandler {
        /** Returns the stable job type string this handler processes. */
        String jobType();

        /**
         * Executes the job. Must be idempotent — may be called more than once for the
         * same job ID if the worker crashes before committing the DONE status.
         *
         * @param jobType the job type string (for handlers that handle multiple types)
         * @param payload the job-specific parameters
         */
        void execute(String jobType, Map<String, Object> payload);
    }
}
