package com.ax.template.authblueprint.scheduledtask;

import java.time.Instant;
import java.util.UUID;

/**
 * REST DTOs for the admin scheduled-task surface.
 * Trace: blueprints/scheduled-task-manifest.yaml#admin_api.
 */
public final class ScheduledTaskDto {

    private ScheduledTaskDto() {}

    public record TaskResponse(
        UUID id,
        String name,
        String cronExpression,
        ScheduledTaskStatus status,
        String handlerBean,
        Instant lastRunAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static TaskResponse from(ScheduledTask t) {
            return new TaskResponse(
                t.getId(), t.getName(), t.getCronExpression(), t.getStatus(),
                t.getHandlerBean(), t.getLastRunAt(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    public record HistoryResponse(
        UUID id,
        String taskName,
        Instant startedAt,
        Instant finishedAt,
        JobOutcome outcome,
        String errorMessage,
        String hostInstance
    ) {
        public static HistoryResponse from(JobHistory h) {
            return new HistoryResponse(
                h.getId(), h.getTaskName(), h.getStartedAt(), h.getFinishedAt(),
                h.getOutcome(), h.getErrorMessage(), h.getHostInstance());
        }
    }

    public record TriggerResponse(boolean executed, HistoryResponse history, String reason) {
        public static TriggerResponse executed(JobHistory h) {
            return new TriggerResponse(true, HistoryResponse.from(h), null);
        }
        public static TriggerResponse skipped(String reason) {
            return new TriggerResponse(false, null, reason);
        }
    }
}
