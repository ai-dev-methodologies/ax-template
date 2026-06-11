package com.ax.template.authblueprint.obligation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One axis governing an obligation's deadline (OBL-AXIS-001). CALENDAR: candidate fixed at
 * anchor + intervalDays. USAGE: candidate re-derives from the remaining budget and the declared
 * consumption rate whenever usage advances (via the package-private {@link #advanceUsage},
 * called by the service under the obligation's row lock). Each (re)derivation is recorded as an
 * appended {@link DerivationRecord} so every deadline is auditable (OBL-GROUND-001).
 */
@AggregateMember(root = Obligation.class)
@Entity
@Table(name = "obligation_axes")
@Check(constraints = "(kind <> 'USAGE') OR (limit_units > 0 AND units_per_day > 0 AND used_units >= 0)")
public class ObligationAxis {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "obligation_id", nullable = false, updatable = false)
    private UUID obligationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    private AxisKind kind;

    @Column(name = "anchor_at", nullable = false, updatable = false)
    private Instant anchorAt;

    /** CALENDAR — interval in days from the anchor. */
    @Column(name = "interval_days")
    private Integer intervalDays;

    /** USAGE — budget, consumption so far, and the declared burn rate the projection uses. */
    @Column(name = "limit_units", precision = 19, scale = 4)
    private BigDecimal limitUnits;

    @Column(name = "used_units", precision = 19, scale = 4)
    private BigDecimal usedUnits;

    @Column(name = "units_per_day", precision = 19, scale = 4)
    private BigDecimal unitsPerDay;

    /** The axis's current candidate deadline — re-derived, never free-typed. */
    @Column(name = "candidate_deadline", nullable = false)
    private Instant candidateDeadline;

    protected ObligationAxis() {}

    static ObligationAxis calendar(UUID id, UUID obligationId, Instant anchorAt, int intervalDays) {
        ObligationAxis a = new ObligationAxis();
        a.id = id;
        a.obligationId = obligationId;
        a.kind = AxisKind.CALENDAR;
        a.anchorAt = anchorAt;
        a.intervalDays = intervalDays;
        a.candidateDeadline = anchorAt.plus(Duration.ofDays(intervalDays));
        return a;
    }

    static ObligationAxis usage(UUID id, UUID obligationId, Instant anchorAt,
                                BigDecimal limitUnits, BigDecimal unitsPerDay) {
        ObligationAxis a = new ObligationAxis();
        a.id = id;
        a.obligationId = obligationId;
        a.kind = AxisKind.USAGE;
        a.anchorAt = anchorAt;
        a.limitUnits = limitUnits;
        a.usedUnits = BigDecimal.ZERO.setScale(4);
        a.unitsPerDay = unitsPerDay;
        a.candidateDeadline = projectFrom(anchorAt, limitUnits, a.usedUnits, unitsPerDay);
        return a;
    }

    /** Package-private — usage advance re-derives the candidate from the NEW remaining budget. */
    Instant advanceUsage(BigDecimal units, Instant now) {
        this.usedUnits = this.usedUnits.add(units);
        this.candidateDeadline = projectFrom(now, limitUnits, usedUnits, unitsPerDay);
        return this.candidateDeadline;
    }

    /** ~100 years — a projection beyond this horizon is an underivable axis, not a date (HIGH-fix). */
    private static final BigDecimal MAX_PROJECTION_SECONDS = BigDecimal.valueOf(3_155_760_000L);

    /** remaining/rate days from {@code from}; an exhausted budget is due immediately. A projection
     *  past the horizon throws the domain 422 BEFORE any Instant/long overflow can reach /error. */
    private static Instant projectFrom(Instant from, BigDecimal limit, BigDecimal used, BigDecimal perDay) {
        BigDecimal remaining = limit.subtract(used);
        if (remaining.signum() <= 0) {
            return from;
        }
        BigDecimal seconds = remaining.multiply(BigDecimal.valueOf(86400))
            .divide(perDay, 0, RoundingMode.HALF_UP);
        if (seconds.compareTo(MAX_PROJECTION_SECONDS) > 0) {
            throw ObligationException.invalidAxis();
        }
        return from.plusSeconds(seconds.longValueExact());
    }

    /** The recomputable formula (OBL-GROUND-001) for the CURRENT candidate. */
    String derivationFormula(Instant from) {
        if (kind == AxisKind.CALENDAR) {
            return "CALENDAR: " + anchorAt + " + P" + intervalDays + "D";
        }
        return "USAGE: " + from + " + (limit " + limitUnits.stripTrailingZeros().toPlainString()
            + " - used " + usedUnits.stripTrailingZeros().toPlainString() + ") / "
            + unitsPerDay.stripTrailingZeros().toPlainString() + " per-day";
    }

    public UUID getId() { return id; }
    public UUID getObligationId() { return obligationId; }
    public AxisKind getKind() { return kind; }
    public Instant getAnchorAt() { return anchorAt; }
    public Integer getIntervalDays() { return intervalDays; }
    public BigDecimal getLimitUnits() { return limitUnits; }
    public BigDecimal getUsedUnits() { return usedUnits; }
    public BigDecimal getUnitsPerDay() { return unitsPerDay; }
    public Instant getCandidateDeadline() { return candidateDeadline; }
}
