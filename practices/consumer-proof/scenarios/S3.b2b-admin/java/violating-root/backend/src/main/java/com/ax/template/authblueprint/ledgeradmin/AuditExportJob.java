package com.ax.template.authblueprint.ledgeradmin;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** AuditExportJob — one enqueued admin export request over the ledger. */
@Entity
public class AuditExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ExportStatus status;

    private String requestedByHash;
    private Instant requestedAt;

    protected AuditExportJob() {
    }

    public AuditExportJob(String requestedByHash, Instant requestedAt) {
        this.status = ExportStatus.QUEUED;
        this.requestedByHash = requestedByHash;
        this.requestedAt = requestedAt;
    }

    public Long getId() {
        return id;
    }

    public ExportStatus getStatus() {
        return status;
    }

    public void markRunning() {
        this.status = ExportStatus.RUNNING;
    }

    public void markDone() {
        this.status = ExportStatus.DONE;
    }

    public void markFailed() {
        this.status = ExportStatus.FAILED;
    }
}
