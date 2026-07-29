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
 *   findByIdForUpdate is the lock-acquisition read — do not substitute findById (see below).
 */
package com.example.app.scheduledtask;

import com.example.app.repositories.BaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ScheduledTask entities.
 *
 * <p>Extends {@link BaseRepository} for soft-delete support.
 *
 * <p>Note: Distributed lock acquisition (tryAcquire) is performed via
 * {@link LockingPolicy.DbRowLockingPolicy}, which reads through
 * {@link #findByIdForUpdate} — a locking query IS needed here. Storing the lock state on
 * the entity row is what makes the lock a read-then-write, not what makes it safe: the
 * plain {@link #findById} + save pattern this note used to describe let two nodes pass the
 * same free-or-stale test on the same row and both acquire (BACKLOG P2-48).
 */
public interface ScheduledTaskRepository extends BaseRepository<ScheduledTask, UUID> {

    /**
     * Lock-acquisition read: takes the task row's pessimistic write lock
     * ({@code SELECT ... FOR UPDATE}) so {@code DbRowLockingPolicy.tryAcquire} can test
     * lock freedom and claim the lock as one indivisible step. A concurrent acquirer blocks
     * here and, when the lock is granted, re-reads the winner's committed lockHolder/lockedAt.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ScheduledTask t WHERE t.id = :taskId")
    Optional<ScheduledTask> findByIdForUpdate(@Param("taskId") UUID taskId);

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
