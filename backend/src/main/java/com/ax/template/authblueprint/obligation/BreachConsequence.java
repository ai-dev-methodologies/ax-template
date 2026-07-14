package com.ax.template.authblueprint.obligation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One BREACH consequence (OBL-CONSEQUENCE-001) — bound exactly once per obligation (the
 * UNIQUE(obligation_id) constraint is the DB backstop), fully immutable. There is deliberately
 * NO stored accrued-interest column: {@link #accruedInterest} DERIVES the amount fresh from the
 * three recorded inputs (basis, statutory rate, days overdue since the obligation's effective
 * deadline) every time it is called (OBL-INTEREST-ACCRUE-001) — the same grounding posture as
 * an axis's candidate deadline, applied to money.
 */
@AggregateMember(root = Obligation.class)
@Entity
@Table(name = "obligation_breach_consequences", uniqueConstraints = {
    @UniqueConstraint(name = "uq_obligation_consequence", columnNames = {"obligation_id"})
})
public class BreachConsequence {

    /** A fixed, documented statutory annual rate (a deliberate simplification of Directive
     *  2011/7/EU's reference-rate-plus-margin mechanism — made explicit here, not overclaimed). */
    static final BigDecimal STATUTORY_ANNUAL_RATE = new BigDecimal("0.08");
    private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");
    private static final int SCALE = 4;

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "obligation_id", nullable = false, updatable = false)
    private UUID obligationId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "basis_amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal basisAmount;

    /** The effective deadline AT the moment the consequence was recorded — interest accrues from
     *  the deadline, not from whenever a sweep happened to notice (OBL-INTEREST-ACCRUE-001). */
    @Column(name = "deadline_at_recording", nullable = false, updatable = false)
    private Instant deadlineAtRecording;

    protected BreachConsequence() {}

    public BreachConsequence(UUID id, UUID obligationId, Instant recordedAt, BigDecimal basisAmount,
                             Instant deadlineAtRecording) {
        this.id = id;
        this.obligationId = obligationId;
        this.recordedAt = recordedAt;
        this.basisAmount = basisAmount;
        this.deadlineAtRecording = deadlineAtRecording;
    }

    /** OBL-INTEREST-ACCRUE-001 — derive-on-read, never a stored running total: basisAmount ×
     *  statutory rate × daysOverdue / 365, recomputed fresh from {@code now} every call. */
    public BigDecimal accruedInterest(Instant now) {
        Duration overdue = Duration.between(deadlineAtRecording, now);
        if (overdue.isNegative()) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        BigDecimal daysOverdue = BigDecimal.valueOf(overdue.toSeconds())
            .divide(BigDecimal.valueOf(86400), 10, RoundingMode.HALF_UP);
        return basisAmount.multiply(STATUTORY_ANNUAL_RATE).multiply(daysOverdue)
            .divide(DAYS_PER_YEAR, SCALE, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public UUID getObligationId() { return obligationId; }
    public Instant getRecordedAt() { return recordedAt; }
    public BigDecimal getBasisAmount() { return basisAmount; }
    public Instant getDeadlineAtRecording() { return deadlineAtRecording; }
}
