package com.ax.template.authblueprint.reportexport;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Orchestration for the report-export domain. Owns the boundary between HTTP
 * concerns (controller) and the state-machine + worker mechanics.
 *
 * <p>Trace:
 * <ul>
 *   <li>EXPORT-LIFECYCLE-001 — {@link #createJob} persists PENDING, returns immediately.</li>
 *   <li>EXPORT-LIFECYCLE-002 — {@link #getJob} returns the current {@code status}.</li>
 *   <li>EXPORT-LIFECYCLE-003 — {@link #download} throws {@link JobNotReadyException}
 *       unless the job is COMPLETED.</li>
 *   <li>EXPORT-AUTHZ-002 / 003 — every lookup uses
 *       {@link ExportJobRepository#findByIdAndOwnerUserId}.</li>
 * </ul>
 */
@Service
public class ReportExportService {

    private final ExportJobRepository repository;
    private final ExportJobStateMachine stateMachine;
    private final ReportExportProperties properties;
    private final ObjectMapper objectMapper;

    public ReportExportService(ExportJobRepository repository,
                               ExportJobStateMachine stateMachine,
                               ReportExportProperties properties,
                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExportJobResponse createJob(String ownerUserId, CreateExportRequest request) {
        ExportFormat fmt = ExportFormat.parse(request.format());
        String queryJson = serializeQuery(request.query());
        ExportJob job = ExportJob.builder()
            .ownerUserId(ownerUserId)
            .format(fmt)
            .name(request.name())
            .status(ExportJobStatus.PENDING)
            .queryJson(queryJson)
            .build();
        ExportJob saved = repository.save(job);
        return ExportJobResponse.from(saved);
    }

    private String serializeQuery(Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(query);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("query is not JSON-serializable", ex);
        }
    }

    @Transactional(readOnly = true)
    public ExportJobResponse getJob(String ownerUserId, UUID jobId) {
        ExportJob job = loadOwned(ownerUserId, jobId);
        return ExportJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public ExportJobListResponse listJobs(String ownerUserId, int page, int size) {
        int clampedSize = Math.min(Math.max(size, 1), 100);
        Page<ExportJob> result =
            repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, PageRequest.of(page, clampedSize));
        return new ExportJobListResponse(
            result.getContent().stream().map(ExportJobResponse::from).toList(),
            page,
            clampedSize,
            result.getTotalElements()
        );
    }

    @Transactional
    public void deleteJob(String ownerUserId, UUID jobId) {
        ExportJob job = loadOwned(ownerUserId, jobId);
        if (job.getStatus() == ExportJobStatus.PENDING) {
            stateMachine.markCancelled(job);
            repository.save(job);
        }
        repository.delete(job);
    }

    /**
     * Returns the COMPLETED job's payload bytes for streaming. Throws:
     * <ul>
     *   <li>{@link ExportJobNotFoundException} (→ 404) when {@code jobId} is not owned
     *       by the caller or does not exist.</li>
     *   <li>{@link JobNotReadyException} (→ 409) when the job exists but is not
     *       COMPLETED.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public DownloadPayload download(String ownerUserId, UUID jobId) {
        ExportJob job = loadOwned(ownerUserId, jobId);
        if (job.getStatus() != ExportJobStatus.COMPLETED) {
            throw new JobNotReadyException("job status is " + job.getStatus() + ", expected COMPLETED");
        }
        byte[] payload = job.getPayload();
        if (payload == null || payload.length == 0) {
            throw new JobNotReadyException("job has no payload (purged or never written)");
        }
        String filename =
            (job.getName() != null && !job.getName().isBlank()
                ? job.getName()
                : "export-" + job.getId())
            + job.getFormat().fileExtension();
        return new DownloadPayload(filename, job.getFormat().contentType(), payload);
    }

    private ExportJob loadOwned(String ownerUserId, UUID jobId) {
        return repository.findByIdAndOwnerUserId(jobId, ownerUserId)
            .orElseThrow(() -> new ExportJobNotFoundException(jobId));
    }

    public int maxRowsPerJob() {
        return properties.getMaxRowsPerJob();
    }

    /** Small carrier so the controller doesn't need to know about the entity. */
    public record DownloadPayload(String filename, String contentType, byte[] bytes) {}
}
