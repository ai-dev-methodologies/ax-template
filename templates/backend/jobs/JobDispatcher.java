/**
 * @ax-template-meta
 * template_id: backend/jobs/JobDispatcher
 * layer: backend-domain
 * domain: jobs
 * anchors_rule: soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Transactional Outbox Pattern (microservices.io) — persist the job record in the same transaction as the domain change; a separate worker polls and executes to guarantee at-least-once delivery"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 *   - source_type: external
 *     citation: "ShedLock GitHub — Distributed lock for scheduled tasks; prevents concurrent execution across multiple application instances without a message broker"
 *     url: "https://github.com/lukas-krecan/ShedLock"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Implement JobDispatcher with a DB-backed JobQueue (see JobQueue.java).
 *   Call dispatch() inside the same @Transactional boundary as the domain write —
 *   transactional outbox guarantees the job is enqueued iff the domain commit succeeds.
 *   The JobWorker polls and executes pending jobs in a separate transaction.
 */
package com.example.app.jobs;

import java.util.Map;

/**
 * Port for dispatching background jobs.
 *
 * <p>Callers enqueue a job by supplying a job type (a stable string identifier) and
 * a payload map. The dispatched job persists in the {@code job_queue} table within
 * the same transaction as the calling domain write (transactional outbox pattern).
 *
 * <p>Job types are application-defined strings (e.g. {@code "send-welcome-email"},
 * {@code "sync-inventory"}). The {@link JobWorker} resolves the correct handler by
 * type via the Spring application context.
 *
 * <h3>Transactional outbox contract</h3>
 * <ul>
 *   <li>{@link #dispatch} must be called inside an active transaction.</li>
 *   <li>If the enclosing transaction rolls back, the job row is also rolled back — no phantom jobs.</li>
 *   <li>If the commit succeeds, the job is guaranteed to be executed at least once by {@link JobWorker}.</li>
 * </ul>
 *
 * <h3>At-least-once delivery</h3>
 * Jobs may be executed more than once if the worker crashes after execution but before
 * marking the job DONE. Job handlers must be idempotent.
 */
public interface JobDispatcher {

    /**
     * Enqueues a job for asynchronous execution.
     *
     * <p>Must be called inside an active transaction. The job row is committed
     * atomically with the enclosing domain write.
     *
     * @param jobType  stable string identifier for the job type (e.g. "send-welcome-email")
     * @param payload  job-specific parameters; must be JSON-serializable
     * @return the ID of the newly created {@code JobQueue} row
     */
    java.util.UUID dispatch(String jobType, Map<String, Object> payload);
}
