/**
 * @ax-template-meta
 * template_id: backend/jobs/JobHistoryProjection
 * layer: backend-application
 * domain: jobs
 * anchors_rule: api-no-entity-leak.md (PRACTICES-API-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Projections: interface-based projections define a subset of entity properties for read-only queries; avoids exposing full entity structure to API consumers"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — @Value in projection interface: SpEL expressions can combine or transform fields without changing the entity or adding a separate DTO layer"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html#projections.interfaces.closed"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Use JobHistoryProjection in JobHistoryRepository queries that feed admin list endpoints.
 *   Projections are read-only — Spring Data optimises the SELECT to only the needed columns.
 *   For full mutation access use the JobHistory entity directly.
 */
package com.example.app.jobs;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

/**
 * Read-only projection for {@link com.example.app.scheduledtask.JobHistory} list endpoints.
 *
 * <p>Exposes the fields needed by admin dashboards without leaking internal entity
 * state (PRACTICES-API-001). Spring Data selects only the projected columns, which
 * reduces I/O on wide entity tables.
 *
 * <p>Usage in a repository:
 * <pre>
 * {@literal @}Query("SELECT j.id AS id, j.taskName AS taskName, j.status AS status, " +
 *        "j.startedAt AS startedAt, j.finishedAt AS finishedAt FROM JobHistory j " +
 *        "WHERE j.taskId = :taskId ORDER BY j.startedAt DESC")
 * Page{@literal <}JobHistoryProjection{@literal >} findByTaskId(UUID taskId, Pageable pageable);
 * </pre>
 *
 * <p>Usage in a controller:
 * <pre>
 * return repository.findByTaskId(taskId, pageable)
 *         .map(p -> new JobHistorySummaryDto(
 *                 p.getId(), p.getTaskName(), p.getStatus().name(),
 *                 p.getStartedAt(), p.getFinishedAt(), p.getDurationMs()));
 * </pre>
 */
public interface JobHistoryProjection {

    UUID getId();
    String getTaskName();

    /**
     * Returns the string name of the {@code JobStatus} enum value.
     * Projections cannot return enum types directly in all Spring Data versions;
     * declare as {@code String} and convert in the DTO layer.
     */
    String getStatus();

    Instant getStartedAt();
    Instant getFinishedAt();

    /**
     * Computed duration in milliseconds between startedAt and finishedAt.
     * Returns {@code null} if the job is still RUNNING (finishedAt not set yet).
     *
     * <p>SpEL expression is evaluated by Spring Data when the projection is populated.
     */
    @Value("#{target.finishedAt != null ? T(java.time.Duration).between(target.startedAt, target.finishedAt).toMillis() : null}")
    Long getDurationMs();
}
