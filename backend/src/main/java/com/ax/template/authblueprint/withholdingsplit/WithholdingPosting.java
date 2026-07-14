package com.ax.template.authblueprint.withholdingsplit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * withholding-split-l0 root: one immutable gross-payment posting. The {@code rate} is the SNAPSHOT
 * that produced the split (WHT-RATE-002) — recorded here, never re-derived from a live policy lookup.
 * {@code correctionOfPostingId} is non-null only for a reversing entry (WHT-IMMUTABLE-004): a
 * correction is a NEW posting (typically negative {@code grossAmount}), never an edit of the original.
 * Every column is {@code updatable = false} and there is NO public setter — the posting is append-only
 * for its entire lifetime. Legs live in {@link WithholdingLeg}, a member written by the service in the
 * SAME transaction.
 */
@AggregateRoot
@Entity
@Table(name = "withholding_postings")
// gross must be non-zero (a zero-gross posting is meaningless); rate is a fraction in [0, 1).
@Check(constraints = "gross_amount <> 0 AND rate >= 0 AND rate < 1")
public class WithholdingPosting {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "gross_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    /** The rate SNAPSHOT that produced this split — the audit answer to "which rate?" (WHT-RATE-002). */
    @Column(name = "rate", nullable = false, updatable = false, precision = 9, scale = 6)
    private BigDecimal rate;

    /** YYYY-MM, declared by the caller (a business period, e.g. a payroll month) — never derived
     *  from the clock, so a posting made today can belong to a prior settlement period. The
     *  remittance-collection scope (WHT-REMIT-003). */
    @Column(name = "period", nullable = false, updatable = false, length = 7)
    private String period;

    /** Non-null only for a reversing entry — the original posting this one corrects (WHT-IMMUTABLE-004). */
    @Column(name = "correction_of_posting_id", updatable = false)
    private UUID correctionOfPostingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WithholdingPosting() {}

    public WithholdingPosting(UUID id, BigDecimal grossAmount, BigDecimal rate, String period,
                              UUID correctionOfPostingId, Instant createdAt) {
        this.id = id;
        this.grossAmount = grossAmount;
        this.rate = rate;
        this.period = period;
        this.correctionOfPostingId = correctionOfPostingId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getRate() { return rate; }
    public String getPeriod() { return period; }
    public UUID getCorrectionOfPostingId() { return correctionOfPostingId; }
    public Instant getCreatedAt() { return createdAt; }
}
