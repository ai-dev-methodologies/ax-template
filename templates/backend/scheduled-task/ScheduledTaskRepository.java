/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/ScheduledTaskRepository
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule: testing-archunit-repository-shape.md (PRACTICES-TEST-004)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Defining Repository Interfaces"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
 *   - source_type: external
 *     citation: "ShedLock — distributed lock for Spring scheduled tasks"
 *     url: "https://github.com/lukas-krecan/ShedLock"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   ScheduledTaskRepository extends BaseRepository for soft-delete support.
 *   Key query: findAllDue(Instant) — used by the processQueue loop to find tasks ready to run.
 */
package com.example.app.scheduledtask;

import com.example.app.repositories.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ScheduledTask entities.
 *
 * <p>Extends {@link BaseRepository} for soft-delete support.
 *
 * <p>Note: Distributed lock acquisition (tryAcquire) is performed via
 * {@link LockingPolicy.DbRowLockingPolicy} which uses this repository's
 * {@link #findById} + save pattern. No special locking queries are needed here
 * because the lock state is part of the entity row.
 */
public interface ScheduledTaskRepository extends BaseRepository<ScheduledTask, UUID> {

    /**
     * Returns all tasks not soft-deleted, for the admin list endpoint.
     */
    @Query("""
        SELECT t FROM ScheduledTask t
        WHERE t.deleted = false
          AND (:status IS NULL OR t.status = :status)
        ORDER BY t.name ASC
        """)
    Page<ScheduledTask> findAllForAdmin(
            @Param("status") ScheduledTask.ScheduledTaskStatus status,
            Pageable pageable);

    /**
     * Returns all non-deleted tasks in REGISTERED or ACTIVE status.
     * Used by the @Scheduled loop to find tasks that should be considered for execution.
     *
     * <p>Note: the loop itself (or LockingPolicy) decides whether to actually run each task.
     */
    @Query("""
        SELECT t FROM ScheduledTask t
        WHERE t.deleted = false
          AND t.status IN ('REGISTERED', 'ACTIVE')
        ORDER BY t.name ASC
        """)
    List<ScheduledTask> findAllEligible();
}
