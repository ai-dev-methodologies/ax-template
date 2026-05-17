/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/ScheduledTaskDto
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "JEP 395 — Records (Final, Java 16)"
 *     url: "https://openjdk.org/jeps/395"
 *   - source_type: external
 *     citation: "OWASP Mass Assignment Cheat Sheet — only expose fields the client is allowed to set"
 *     url: "https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All inner records are Java 16 records — immutable value types.
 *   Map at service layer: Summary.from(ScheduledTask), Detail.from(ScheduledTask),
 *   JobHistorySummary.from(JobHistory).
 */
package com.example.app.scheduledtask;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO container for the scheduled-task domain.
 *
 * <p>Inner records:
 * <ul>
 *   <li>{@link Summary}           — list response (lightweight)
 *   <li>{@link Detail}            — single-task response (includes lock state)
 *   <li>{@link JobHistorySummary} — job history list item
 * </ul>
 */
public final class ScheduledTaskDto {

    private ScheduledTaskDto() {}

    // ─── task response DTOs ──────────────────────────────────────────────

    /**
     * Lightweight summary for list endpoint.
     */
    public record Summary(
        UUID id,
        String name,
        String cronExpression,
        ScheduledTask.ScheduledTaskStatus status,
        Instant lastRunAt,
        Instant createdAt
    ) {
        public static Summary from(ScheduledTask t) {
            return new Summary(
                t.getId(),
                t.getName(),
                t.getCronExpression(),
                t.getStatus(),
                t.getLastRunAt(),
                t.getCreatedAt()
            );
        }
    }

    /**
     * Full detail including current lock state (for admin single-task view).
     */
    public record Detail(
        UUID id,
        String name,
        String cronExpression,
        ScheduledTask.ScheduledTaskStatus status,
        Instant lastRunAt,
        String lockHolder,
        Instant lockedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static Detail from(ScheduledTask t) {
            return new Detail(
                t.getId(),
                t.getName(),
                t.getCronExpression(),
                t.getStatus(),
                t.getLastRunAt(),
                t.getLockHolder(),
                t.getLockedAt(),
                t.getCreatedAt(),
                t.getUpdatedAt()
            );
        }
    }

    // ─── history response ────────────────────────────────────────────────

    /**
     * Job history list item.
     */
    public record JobHistorySummary(
        UUID id,
        UUID taskId,
        String taskName,
        JobHistory.JobStatus status,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage
    ) {
        public static JobHistorySummary from(JobHistory h) {
            return new JobHistorySummary(
                h.getId(),
                h.getTaskId(),
                h.getTaskName(),
                h.getStatus(),
                h.getStartedAt(),
                h.getFinishedAt(),
                h.getErrorMessage()
            );
        }
    }
}
