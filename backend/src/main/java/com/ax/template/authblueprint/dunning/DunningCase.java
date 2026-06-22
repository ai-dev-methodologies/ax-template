package com.ax.template.authblueprint.dunning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * dunning-collections-l0 root: one overdue receivable walked through a one-way notice ladder
 * (DUNNING-LADDER-001), classified into a recorded aging bucket (DUNNING-AGING-001), and
 * subject to a cure-period grace window (DUNNING-CURE-001). Lifecycle moves ONLY via the
 * package-private hooks, called by {@link DunningService} under the case's PESSIMISTIC_WRITE
 * row lock (DUNNING-CONCURRENT-001). The aging columns carry their own basis (the as-of
 * instant + days-overdue) so a bare bucket label is unrepresentable.
 */
@AggregateRoot
@Entity
@Table(name = "dunning_cases")
@Check(constraints =
    "overdue_amount >= 0 AND paid_amount >= 0"
    + " AND (cure_deadline IS NULL OR cure_window_opened_at IS NOT NULL)"
    + " AND (ladder_halted = FALSE OR aging_bucket = 'CURRENT')")
public class DunningCase {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The receivable's external reference (invoice/account id) — opaque, recorded verbatim. */
    @Column(name = "receivable_ref", nullable = false, updatable = false, length = 200)
    private String receivableRef;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "overdue_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal overdueAmount;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private DunningStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "aging_bucket", nullable = false, length = 20)
    private AgingBucket agingBucket;

    /** The recorded basis for {@link #agingBucket} — re-appraisable, never a bare label. */
    @Column(name = "aging_as_of")
    private Instant agingAsOf;

    @Column(name = "days_overdue", nullable = false)
    private long daysOverdue;

    @Column(name = "cure_window_opened_at")
    private Instant cureWindowOpenedAt;

    @Column(name = "cure_deadline")
    private Instant cureDeadline;

    @Column(name = "ladder_halted", nullable = false)
    private boolean ladderHalted;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DunningCase() {}

    public DunningCase(UUID id, String receivableRef, LocalDate dueDate, BigDecimal overdueAmount,
                       Instant createdAt) {
        this.id = id;
        this.receivableRef = receivableRef;
        this.dueDate = dueDate;
        this.overdueAmount = overdueAmount;
        this.paidAmount = BigDecimal.ZERO;
        this.stage = DunningStage.REMINDER;
        this.agingBucket = AgingBucket.CURRENT;
        this.daysOverdue = 0L;
        this.ladderHalted = false;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — advance one rung along the one-way ladder (DUNNING-LADDER-001). */
    void advanceTo(DunningStage nextStage) {
        this.stage = nextStage;
        this.ladderHalted = false;
    }

    /** Sole-mutator hook — record the deterministic aging classification + its basis (DUNNING-AGING-001). */
    void reage(AgingBucket bucket, Instant asOf, long days) {
        this.agingBucket = bucket;
        this.agingAsOf = asOf;
        this.daysOverdue = days;
    }

    /** Sole-mutator hook — open (or keep open) the cure window with a recorded deadline (DUNNING-CURE-001). */
    void openCureWindow(Instant openedAt, Instant deadline) {
        if (this.cureWindowOpenedAt == null) {
            this.cureWindowOpenedAt = openedAt;
        }
        this.cureDeadline = deadline;
    }

    /** Sole-mutator hook — record a payment toward the overdue amount (DUNNING-CURE-001). */
    void addPayment(BigDecimal amount) {
        this.paidAmount = this.paidAmount.add(amount);
    }

    /** Sole-mutator hook — full cure: aging back to CURRENT, halt the ladder, close the window.
     *  The stage pointer is NOT rewound — a lapse resumes the one-way ladder where it left off,
     *  keeping each rung's exactly-once transition intact (DUNNING-CURE-001). */
    void cure() {
        this.agingBucket = AgingBucket.CURRENT;
        this.daysOverdue = 0L;
        this.ladderHalted = true;
        this.cureWindowOpenedAt = null;
        this.cureDeadline = null;
    }

    /** Sole-mutator hook — a lapse releases the halt so the ladder may resume forward. */
    void releaseHalt() {
        this.ladderHalted = false;
    }

    public boolean isFullyPaid() {
        return paidAmount.compareTo(overdueAmount) >= 0;
    }

    public UUID getId() { return id; }
    public String getReceivableRef() { return receivableRef; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public DunningStage getStage() { return stage; }
    public AgingBucket getAgingBucket() { return agingBucket; }
    public Instant getAgingAsOf() { return agingAsOf; }
    public long getDaysOverdue() { return daysOverdue; }
    public Instant getCureWindowOpenedAt() { return cureWindowOpenedAt; }
    public Instant getCureDeadline() { return cureDeadline; }
    public boolean isLadderHalted() { return ladderHalted; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
