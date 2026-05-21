package com.ax.template.authblueprint.reportexport;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire-format projection of {@link ExportJob}.
 *
 * <p>{@code downloadAvailable} is derived (status == COMPLETED && payload present)
 * so clients can branch on a single boolean instead of duplicating the rule.
 */
public record ExportJobResponse(
    UUID jobId,
    ExportJobStatus status,
    ExportFormat format,
    String name,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    Long rowCount,
    Long sizeBytes,
    String errorMessage,
    boolean downloadAvailable
) {

    public static ExportJobResponse from(ExportJob job) {
        boolean available =
            job.getStatus() == ExportJobStatus.COMPLETED
            && job.getPayload() != null
            && job.getPayload().length > 0;
        return new ExportJobResponse(
            job.getId(),
            job.getStatus(),
            job.getFormat(),
            job.getName(),
            job.getCreatedAt(),
            job.getStartedAt(),
            job.getCompletedAt(),
            job.getRowCount(),
            job.getSizeBytes(),
            job.getErrorMessage(),
            available
        );
    }
}
