package com.ax.template.authblueprint.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * external-reconciliation-l0 root: one reconciliation of an internal record set against an
 * external feed snapshot. Identified by (sourceKey, feedSnapshotHash) so a re-run on the SAME
 * feed returns this run verbatim and a CHANGED feed appends a new run, prior retained
 * (RECON-IDEMPOTENT-001). The classified pairs live as {@link ReconciliationItem} members, each
 * carrying its own basis (internal/external amount + delta) so a bare aggregate count is
 * unrepresentable (RECON-CLASSIFY-001). The run moves to RESOLVED ONLY via the package-private
 * {@link #resolve(Instant)} hook, called by {@link ReconciliationService} after it has verified
 * every break is disposed (RECON-RESOLVE-001). No delete path exists — runs are append-only.
 */
@AggregateRoot
@Entity
@Table(name = "reconciliation_runs", uniqueConstraints = {
    @UniqueConstraint(name = "uq_recon_source_feed", columnNames = {"source_key", "feed_snapshot_hash"})
})
public class ReconciliationRun {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The reconciliation source (e.g. custodian/bank/processor account) — opaque, recorded verbatim. */
    @Column(name = "source_key", nullable = false, updatable = false, length = 200)
    private String sourceKey;

    /** The content hash of the external feed snapshot — the idempotency identity (RECON-IDEMPOTENT-001). */
    @Column(name = "feed_snapshot_hash", nullable = false, updatable = false, length = 200)
    private String feedSnapshotHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReconciliationStatus status;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReconciliationRun() {}

    public ReconciliationRun(UUID id, String sourceKey, String feedSnapshotHash, Instant createdAt) {
        this.id = id;
        this.sourceKey = sourceKey;
        this.feedSnapshotHash = feedSnapshotHash;
        this.status = ReconciliationStatus.OPEN;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — mark the run RESOLVED once every break is disposed (RECON-RESOLVE-001). */
    void resolve(Instant at) {
        this.status = ReconciliationStatus.RESOLVED;
        this.resolvedAt = at;
    }

    public boolean isResolved() {
        return status == ReconciliationStatus.RESOLVED;
    }

    public UUID getId() { return id; }
    public String getSourceKey() { return sourceKey; }
    public String getFeedSnapshotHash() { return feedSnapshotHash; }
    public ReconciliationStatus getStatus() { return status; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
