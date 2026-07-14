package com.ax.template.authblueprint.thresholdfiling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * threshold-filing-obligation-l0 filing record (TFO-TRIGGER-001/TFO-FILING-RECORD-001) — bound
 * exactly once per register (UNIQUE(register_id) is the DB backstop). {@code subjectKey} /
 * {@code thresholdSnapshot} / {@code triggerInstant} / {@code dueAt} are immutable — self-describing
 * provenance that survives even if the register's own state later changes. Only {@code status} /
 * {@code ackBy} / {@code ackAt} ever mutate, and ONLY via the package-private {@link #acknowledge}
 * (TFO-DEADLINE-001 — the ONLY terminal writer; there is no expire/silent-close path).
 */
@AggregateMember(root = FilingRegister.class)
@Entity
@Table(name = "filing_obligations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_filing_obligation_register", columnNames = {"register_id"})
})
@Check(constraints = "status <> 'ACKNOWLEDGED' OR (ack_by IS NOT NULL AND ack_at IS NOT NULL)")
public class FilingObligation {

    /** 31 CFR §1020.320(b)(3) — the fixed statutory SAR filing window: "no later than 30 calendar
     *  days after the date of initial detection". A single, unambiguous figure (unlike FATF Rec 20,
     *  which leaves the exact window to national law) — documented here, not silently assumed. */
    static final long FILING_WINDOW_DAYS = 30;

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "register_id", nullable = false, updatable = false)
    private UUID registerId;

    @Column(name = "subject_key", nullable = false, updatable = false, length = 200)
    private String subjectKey;

    @Column(name = "threshold_snapshot", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal thresholdSnapshot;

    @Column(name = "trigger_instant", nullable = false, updatable = false)
    private Instant triggerInstant;

    @Column(name = "due_at", nullable = false, updatable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FilingObligationStatus status;

    @Column(name = "ack_by", length = 200)
    private String ackBy;

    @Column(name = "ack_at")
    private Instant ackAt;

    protected FilingObligation() {}

    public FilingObligation(UUID id, UUID registerId, String subjectKey, BigDecimal thresholdSnapshot,
                            Instant triggerInstant) {
        this.id = id;
        this.registerId = registerId;
        this.subjectKey = subjectKey;
        this.thresholdSnapshot = thresholdSnapshot;
        this.triggerInstant = triggerInstant;
        this.dueAt = triggerInstant.plus(java.time.Duration.ofDays(FILING_WINDOW_DAYS));
        this.status = FilingObligationStatus.OPEN;
    }

    /** The ONLY terminal writer (TFO-DEADLINE-001) — records who closed the loop, and when. */
    void acknowledge(String by, Instant at) {
        this.status = FilingObligationStatus.ACKNOWLEDGED;
        this.ackBy = by;
        this.ackAt = at;
    }

    public UUID getId() { return id; }
    public UUID getRegisterId() { return registerId; }
    public String getSubjectKey() { return subjectKey; }
    public BigDecimal getThresholdSnapshot() { return thresholdSnapshot; }
    public Instant getTriggerInstant() { return triggerInstant; }
    public Instant getDueAt() { return dueAt; }
    public FilingObligationStatus getStatus() { return status; }
    public String getAckBy() { return ackBy; }
    public Instant getAckAt() { return ackAt; }
}
