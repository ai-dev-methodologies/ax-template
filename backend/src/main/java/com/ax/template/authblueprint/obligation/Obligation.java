package com.ax.template.authblueprint.obligation;

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
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * deadline-obligation-l0 root: one governed deadline obligation. {@code effectiveDeadline} is
 * ALWAYS derived — min of the axis candidates (OBL-GROUND/AXIS-001), re-evaluated only via the
 * package-private {@link #reevaluate} under the row lock. The ONLY terminal edge is
 * {@link #acknowledge} (OBL-ACK-001) — there is no expire/cancel mutator, so the sweep cannot
 * close a loop nobody closed. The @Check backstops that an ACKNOWLEDGED row carries who/when.
 */
@AggregateRoot
@Entity
@Table(name = "deadline_obligations")
@Check(constraints = "status <> 'ACKNOWLEDGED' OR (ack_by IS NOT NULL AND ack_at IS NOT NULL)")
public class Obligation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "obligation_key", nullable = false, updatable = false, length = 200, unique = true)
    private String obligationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ObligationStatus status;

    /** Derived — min of the axis candidates; never accepted raw from a caller. */
    @Column(name = "effective_deadline", nullable = false)
    private Instant effectiveDeadline;

    /** The ladder window's start — the EARLIEST axis anchor (the obligation's true lifecycle
     *  start, which may precede registration; OBL-LADDER-001 paces rungs over windowStart→deadline). */
    @Column(name = "window_start", nullable = false, updatable = false)
    private Instant windowStart;

    @Column(name = "ack_by", length = 200)
    private String ackBy;

    @Column(name = "ack_at")
    private Instant ackAt;

    /** OBL-CONSEQUENCE-001 — the OPTIONAL basis a BREACH consequence is measured against;
     *  declared once at creation, never a mutable "amount owed" the sweep must keep in sync. */
    @Column(name = "breach_basis_amount", updatable = false, precision = 19, scale = 4)
    private BigDecimal breachBasisAmount;

    /** OBL-WAIVER-001 — the SAME dual-axis shape as OBL-AXIS-001, applied to a waiver's validity
     *  window: how many usage-advance events this obligation has seen so far. */
    @Column(name = "usage_cycle_count", nullable = false)
    private long usageCycleCount;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Obligation() {}

    public Obligation(UUID id, String obligationKey, Instant effectiveDeadline, Instant windowStart,
                      Instant createdAt) {
        this(id, obligationKey, effectiveDeadline, windowStart, createdAt, null);
    }

    public Obligation(UUID id, String obligationKey, Instant effectiveDeadline, Instant windowStart,
                      Instant createdAt, BigDecimal breachBasisAmount) {
        this.id = id;
        this.obligationKey = obligationKey;
        this.status = ObligationStatus.OPEN;
        this.effectiveDeadline = effectiveDeadline;
        this.windowStart = windowStart;
        this.createdAt = createdAt;
        this.breachBasisAmount = breachBasisAmount;
        this.usageCycleCount = 0;
    }

    /** Sole-mutator hook (service, under the row lock) — the deadline is always min(candidates). */
    void reevaluate(Instant newEffectiveDeadline) {
        this.effectiveDeadline = newEffectiveDeadline;
    }

    /** The ONLY terminal writer (OBL-ACK-001) — records who closed the loop, and when. */
    void acknowledge(String by, Instant at) {
        this.status = ObligationStatus.ACKNOWLEDGED;
        this.ackBy = by;
        this.ackAt = at;
    }

    /** Sole-mutator hook (service, under the row lock) — one usage-cycle event has occurred;
     *  this is the axis a waiver's expiresAfterCycles bound is measured against (OBL-WAIVER-001). */
    void incrementUsageCycle() {
        this.usageCycleCount++;
    }

    public UUID getId() { return id; }
    public String getObligationKey() { return obligationKey; }
    public ObligationStatus getStatus() { return status; }
    public Instant getEffectiveDeadline() { return effectiveDeadline; }
    public Instant getWindowStart() { return windowStart; }
    public String getAckBy() { return ackBy; }
    public Instant getAckAt() { return ackAt; }
    public BigDecimal getBreachBasisAmount() { return breachBasisAmount; }
    public long getUsageCycleCount() { return usageCycleCount; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
