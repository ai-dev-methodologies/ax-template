package com.ax.template.authblueprint.auditlog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Application service for asynchronous export jobs.
 * <p>
 * Trace: AUDIT-EXPORT-001 — enqueue + poll. The actual export processing is
 * outside the scope of the catalog (depends on the fork-receiver's blob store).
 * This service persists the job row and returns the ID immediately.
 */
@Service
public class AuditLogExportService {

    private final AuditExportJobRepository jobRepository;

    public AuditLogExportService(AuditExportJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public AuditExportJob enqueue(String requestedBy, AuditExportFormat format, String filterJson) {
        AuditExportJob job = new AuditExportJob(
            UUID.randomUUID(),
            requestedBy,
            format,
            AuditExportStatus.PENDING,
            filterJson
        );
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Optional<AuditExportJob> findById(UUID id) {
        return jobRepository.findById(id);
    }
}
