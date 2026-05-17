/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/JobHistoryRepository
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule: testing-archunit-repository-shape.md (PRACTICES-TEST-004)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Defining Repository Interfaces"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
 *   - source_type: external
 *     citation: "12-Factor App — Factor XI: Logs (treat logs as event streams)"
 *     url: "https://12factor.net/logs"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   JobHistoryRepository extends BaseRepository for soft-delete support.
 *   Provides queries for admin history view and TTL-based pruning.
 */
package com.example.app.scheduledtask;

import com.example.app.repositories.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for JobHistory entities.
 *
 * <p>Extends {@link BaseRepository} for soft-delete support.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link #findByTaskId} — paginated history for one task (admin endpoint)
 *   <li>{@link #pruneOlderThan} — bulk delete old records for retention enforcement
 * </ul>
 */
public interface JobHistoryRepository extends BaseRepository<JobHistory, UUID> {

    /**
     * Returns paginated job history for a specific task, newest first.
     *
     * @param taskId   UUID of the ScheduledTask
     * @param pageable page / size from request params
     */
    @Query("""
        SELECT h FROM JobHistory h
        WHERE h.taskId = :taskId
          AND h.deleted = false
        ORDER BY h.startedAt DESC
        """)
    Page<JobHistory> findByTaskId(@Param("taskId") UUID taskId, Pageable pageable);

    /**
     * Hard-deletes job history records older than the given cutoff instant.
     * Used by the retention cleanup task (default: 90 days).
     *
     * @param cutoff records with startedAt before this instant are removed
     * @return number of records deleted
     */
    @Modifying
    @Query("DELETE FROM JobHistory h WHERE h.startedAt < :cutoff")
    int pruneOlderThan(@Param("cutoff") Instant cutoff);
}
