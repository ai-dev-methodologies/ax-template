package com.ax.template.authblueprint.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One classified internal/external pair within a {@link ReconciliationRun} (RECON-CLASSIFY-001).
 * The classification and its BASIS (the internal amount, the external amount, the delta) are
 * recorded immutably at run time — a bare aggregate count is unrepresentable. Only a BREAK can
 * be DISPOSED (RECON-DISPOSE-001); the disposition fields (type/by/at/reason) are written ONCE,
 * under the item's PESSIMISTIC_WRITE row lock, by {@link ReconciliationService}. The @Check
 * backstops make a disposed non-BREAK and a half-written disposition unrepresentable. The item
 * is append-only one-per-(run, key) via uq(run_id, item_key); no delete path exists.
 */
@AggregateMember(root = ReconciliationRun.class)
@Entity
@Table(name = "reconciliation_items", uniqueConstraints = {
    @UniqueConstraint(name = "uq_recon_run_item", columnNames = {"run_id", "item_key"})
})
@Check(constraints =
    "(disposed = FALSE OR classification = 'BREAK')"
    + " AND (disposed = FALSE OR (disposition_type IS NOT NULL"
    + " AND disposed_by IS NOT NULL AND disposed_at IS NOT NULL AND disposition_reason IS NOT NULL))")
public class ReconciliationItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    /** The reconciliation key for this pair (e.g. transaction/position id) — opaque, recorded verbatim. */
    @Column(name = "item_key", nullable = false, updatable = false, length = 200)
    private String itemKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, updatable = false, length = 20)
    private ItemClassification classification;

    /** The internal-side amount; null when the key is absent internally (EXTERNAL_ONLY). */
    @Column(name = "internal_amount", updatable = false, precision = 19, scale = 4)
    private BigDecimal internalAmount;

    /** The external-side amount; null when the key is absent externally (INTERNAL_ONLY). */
    @Column(name = "external_amount", updatable = false, precision = 19, scale = 4)
    private BigDecimal externalAmount;

    /** internal minus external (basis for a BREAK); null when one side is absent. */
    @Column(name = "delta", updatable = false, precision = 19, scale = 4)
    private BigDecimal delta;

    @Column(name = "disposed", nullable = false)
    private boolean disposed;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition_type", length = 20)
    private DispositionType dispositionType;

    @Column(name = "disposed_by", length = 200)
    private String disposedBy;

    @Column(name = "disposed_at")
    private Instant disposedAt;

    @Column(name = "disposition_reason", length = 1000)
    private String dispositionReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReconciliationItem() {}

    public ReconciliationItem(UUID id, UUID runId, String itemKey, BigDecimal internalAmount,
                              BigDecimal externalAmount, Instant createdAt) {
        this.id = id;
        this.runId = runId;
        this.itemKey = itemKey;
        this.internalAmount = internalAmount;
        this.externalAmount = externalAmount;
        this.classification = ItemClassification.of(internalAmount, externalAmount);
        this.delta = (internalAmount != null && externalAmount != null)
            ? internalAmount.subtract(externalAmount) : null;
        this.disposed = false;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — record the human disposition of this BREAK exactly once (RECON-DISPOSE-001). */
    void dispose(DispositionType type, String by, Instant at, String reason) {
        this.disposed = true;
        this.dispositionType = type;
        this.disposedBy = by;
        this.disposedAt = at;
        this.dispositionReason = reason;
    }

    public boolean isBreak() {
        return classification == ItemClassification.BREAK;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public String getItemKey() { return itemKey; }
    public ItemClassification getClassification() { return classification; }
    public BigDecimal getInternalAmount() { return internalAmount; }
    public BigDecimal getExternalAmount() { return externalAmount; }
    public BigDecimal getDelta() { return delta; }
    public boolean isDisposed() { return disposed; }
    public DispositionType getDispositionType() { return dispositionType; }
    public String getDisposedBy() { return disposedBy; }
    public Instant getDisposedAt() { return disposedAt; }
    public String getDispositionReason() { return dispositionReason; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
