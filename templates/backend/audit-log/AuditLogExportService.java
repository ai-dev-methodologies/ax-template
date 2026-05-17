/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLogExportService
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: specs/audit-log-l0.yaml#AUDIT-EXPORT-001
 *               specs/audit-log-l0.yaml#AUDIT-EXPORT-002
 *               contracts/audit-log-openapi.yaml#exportAuditLogs
 *               blueprints/audit-log-manifest.yaml#export
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Reference — @Async for non-blocking background execution"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Streaming large result sets with ScrollPosition"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/repositories/scrolling.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   enqueue() returns immediately with a job ID.
 *   processExport() runs @Async and writes to a temp file, then updates job status.
 *   For production: replace in-memory job store with a database-backed JobRepository.
 */
package com.example.app.auditlog;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuditLogExportService — manages async export jobs for bulk audit log downloads.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #enqueue} creates a job in PENDING state and triggers async processing.
 *   <li>{@link #processExport} queries the audit log, writes the output, and transitions
 *       the job to COMPLETED (with a download URL) or FAILED.
 *   <li>{@link #getStatus} lets the caller poll the current job state.
 * </ol>
 *
 * <p>Production note: Replace the in-memory {@code jobStore} with a persistent
 * {@code ExportJobRepository} (JPA entity) so jobs survive restarts and scale across pods.
 */
@Service
public class AuditLogExportService {

    private final AuditLogRepository auditLogRepository;

    /** In-memory job store — replace with JPA repository in production. */
    private final Map<UUID, ExportJobStatus> jobStore = new ConcurrentHashMap<>();

    public AuditLogExportService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // ─── Enqueue ─────────────────────────────────────────────────────────────

    /**
     * Enqueues a new export job and returns immediately with PENDING status.
     *
     * @param request export filter + format
     * @return job ID for polling
     */
    public ExportJobResponse enqueue(ExportRequest request) {
        UUID jobId = UUID.randomUUID();
        ExportJobStatus pending = new ExportJobStatus(jobId, JobState.PENDING, null, null, null);
        jobStore.put(jobId, pending);
        processExport(jobId, request);   // fires async
        return new ExportJobResponse(jobId, JobState.PENDING);
    }

    // ─── Async processing ────────────────────────────────────────────────────

    /**
     * Background export processor (async).
     *
     * <p>Fork instructions:
     *   1. Replace in-memory jobStore with JPA-backed ExportJobRepository.
     *   2. Write output to object storage (S3, GCS, MinIO) and generate a pre-signed URL.
     *   3. Configure a thread pool in your application context for @Async.
     *   4. Add progress tracking for very large exports (500k+ rows).
     */
    @Async
    protected void processExport(UUID jobId, ExportRequest request) {
        updateJobState(jobId, JobState.PROCESSING, null, null, null);
        try {
            // TODO: stream records from auditLogRepository using Specification from request filters
            // TODO: serialize to CSV or JSON depending on request.format()
            // TODO: upload to object storage, generate pre-signed URL valid for 1 hour
            long recordCount = countMatchingRecords(request);
            String downloadUrl = generateDownloadUrl(jobId, request.format());
            updateJobState(jobId, JobState.COMPLETED, downloadUrl, null, recordCount);
        } catch (Exception e) {
            updateJobState(jobId, JobState.FAILED, null, e.getMessage(), null);
        }
    }

    // ─── Poll ────────────────────────────────────────────────────────────────

    /**
     * Returns the current status of an export job.
     *
     * @param jobId export job UUID
     * @return job status, or empty if not found
     */
    public Optional<ExportJobStatus> getStatus(UUID jobId) {
        return Optional.ofNullable(jobStore.get(jobId));
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void updateJobState(UUID jobId, JobState state, String downloadUrl,
                                String errorMessage, Long recordCount) {
        jobStore.put(jobId, new ExportJobStatus(jobId, state, downloadUrl, errorMessage, recordCount));
    }

    private long countMatchingRecords(ExportRequest request) {
        // Placeholder — replace with actual repository count using Specification
        return auditLogRepository.count();
    }

    private String generateDownloadUrl(UUID jobId, String format) {
        // Placeholder — replace with pre-signed URL from object storage
        // URL must be valid for 1 hour (blueprints/audit-log-manifest.yaml#export)
        return "/api/audit-logs/export/" + jobId + "/download?format=" + format.toLowerCase();
    }

    // ─── DTO types ───────────────────────────────────────────────────────────

    public enum JobState { PENDING, PROCESSING, COMPLETED, FAILED }

    public record ExportRequest(
        @NotNull String format,              // CSV or JSON
        String actorId,
        String resourceType,
        String action,
        AuditLog.Outcome outcome,
        Instant from,
        Instant to
    ) {}

    public record ExportJobResponse(
        UUID jobId,
        JobState status
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExportJobStatus(
        UUID jobId,
        JobState status,
        String downloadUrl,     // present when COMPLETED
        String errorMessage,    // present when FAILED
        Long recordCount        // present when COMPLETED
    ) {}
}
