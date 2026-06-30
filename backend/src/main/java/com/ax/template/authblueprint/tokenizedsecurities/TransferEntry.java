package com.ax.template.authblueprint.tokenizedsecurities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/** Append-only 이전 기록 — 한 번 기록되면 불변. */
@AggregateMember(root = SecurityTokenRegister.class)
@Entity
@Table(name = "transfer_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_transfer_entry_transfer_id",
                columnNames = {"register_id", "transfer_id"}))
public class TransferEntry {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "register_id", nullable = false, updatable = false)
    private SecurityTokenRegister register;

    @Column(name = "from_holder_id", nullable = false, updatable = false, length = 200)
    private String fromHolderId;

    @Column(name = "to_holder_id", nullable = false, updatable = false, length = 200)
    private String toHolderId;

    @Column(name = "units", nullable = false, updatable = false)
    private long units;

    @Column(name = "transfer_id", nullable = false, updatable = false, length = 200)
    private String transferId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    /** ANCHOR-001 — tx-ref returned by OnChainAnchor.anchor(); immutable once set. */
    @Column(name = "anchor_ref", nullable = false, updatable = false, length = 200)
    private String anchorRef;

    protected TransferEntry() {}

    TransferEntry(SecurityTokenRegister register, String fromHolderId, String toHolderId,
                  long units, String transferId, Instant recordedAt, String anchorRef) {
        this.id = UUID.randomUUID();
        this.register = register;
        this.fromHolderId = fromHolderId;
        this.toHolderId = toHolderId;
        this.units = units;
        this.transferId = transferId;
        this.recordedAt = recordedAt;
        this.anchorRef = anchorRef;
    }

    public UUID getId() { return id; }
    public String getFromHolderId() { return fromHolderId; }
    public String getToHolderId() { return toHolderId; }
    public long getUnits() { return units; }
    public String getTransferId() { return transferId; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getAnchorRef() { return anchorRef; }
}
