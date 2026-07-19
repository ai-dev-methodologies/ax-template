package com.ax.template.authblueprint.ledgeradmin;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * AuditExportService — sole mutator of AuditExportJob (state-machine
 * discipline consistent with report-export / email-outbox L4 domains).
 * Controllers route here; they never touch AuditLedgerRepository directly.
 */
@Service
public class AuditExportService {

    private final AuditLedgerRepository ledgerRepository;
    private final Map<Long, AuditExportJob> jobs = new HashMap<>();
    private long nextJobId = 1L;

    public AuditExportService(AuditLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public Page<AuditLedgerEntry> listLedger(String entityRef, Pageable pageable) {
        return ledgerRepository.findAllByEntityRef(entityRef, pageable);
    }

    public AuditExportJob enqueueExport(String requestedByHash) {
        AuditExportJob job = new AuditExportJob(requestedByHash, Instant.now());
        Long id = nextJobId++;
        jobs.put(id, job);
        return job;
    }

    public AuditExportJob getStatus(Long jobId) {
        AuditExportJob job = jobs.get(jobId);
        if (job == null) {
            throw new LedgerJobNotFoundException(jobId);
        }
        return job;
    }
}
