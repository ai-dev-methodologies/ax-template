package com.ax.template.authblueprint.correctionrefire;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * correction-refire-l0 — the acknowledgement state of EXACTLY ONE {@link CorrectedRecord} version
 * (CRF-CHAIN-004: each version's ack state is tracked and queried independently). Created PENDING
 * in the SAME transaction every time a version is published (whether the first version or a
 * correction) — CRF-REFIRE-002's "re-open the loop" behavior falls out naturally: a correction
 * over a version whose ack was CLOSED gets a brand-new PENDING row for the corrected version,
 * while the prior version's CLOSED row is never touched (a distinct row, never revisited).
 * {@code @AggregateMember} of {@link CorrectedRecord}: {@code common/MemberWriter} writes,
 * root-JPQL reads.
 */
@AggregateMember(root = CorrectedRecord.class)
@Entity
@Table(name = "correction_ack_records", uniqueConstraints = {
    @UniqueConstraint(name = "uq_ack_record_id", columnNames = {"record_id"})
})
public class AckRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The {@link CorrectedRecord#getId()} this ack tracks — a 1:1 companion row. */
    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AckStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected AckRecord() {}

    AckRecord(UUID id, UUID recordId, Instant createdAt) {
        this.id = id;
        this.recordId = recordId;
        this.status = AckStatus.PENDING;
        this.createdAt = createdAt;
    }

    public static AckRecord pending(UUID id, UUID recordId, Instant createdAt) {
        return new AckRecord(id, recordId, createdAt);
    }

    /** Sole-mutator hook — called ONLY by {@link CorrectionRefireService#acknowledge}. */
    void close(Instant closedAt) {
        this.status = AckStatus.CLOSED;
        this.closedAt = closedAt;
    }

    public UUID getId() { return id; }
    public UUID getRecordId() { return recordId; }
    public AckStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
}
