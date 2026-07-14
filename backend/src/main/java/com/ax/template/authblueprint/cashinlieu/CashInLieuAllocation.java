package com.ax.template.authblueprint.cashinlieu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * cash-in-lieu-l0 root: one immutable, FROZEN entitlement allocation for one (subjectRef, eventRef)
 * pair (CIL-IDEMPOTENT-003 — {@code uq(subject_ref, event_ref)}). {@code rawEntitlement =
 * holdingQuantity * ratio} splits into an integer {@code unitsInKind} (floor) and a
 * {@code fractionalRemainder} that is NEVER allocated in kind — it is converted to {@code cashValue}
 * at the {@code cashRate} SNAPSHOT recorded here immutably (CIL-FRACTION-001). {@code unitsInKind +
 * fractionalRemainder == rawEntitlement} EXACTLY (pure subtraction, no rounding — CIL-CONSERVE-002);
 * the currency rounding of {@code cashValue} is a separate, explicit step. Every column is
 * {@code updatable = false} and there is no public setter — a re-allocation request for the SAME
 * (subject, event) returns this frozen row, it never edits it.
 */
@AggregateRoot
@Entity
@Table(name = "cash_in_lieu_allocations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_cil_subject_event", columnNames = {"subject_ref", "event_ref"})
})
@Check(constraints = "units_in_kind >= 0 AND fractional_remainder >= 0 AND fractional_remainder < 1"
    + " AND cash_rate > 0 AND cash_value >= 0")
public class CashInLieuAllocation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_ref", nullable = false, updatable = false, length = 200)
    private String subjectRef;

    @Column(name = "event_ref", nullable = false, updatable = false, length = 200)
    private String eventRef;

    @Column(name = "raw_entitlement", nullable = false, updatable = false, precision = 24, scale = 6)
    private BigDecimal rawEntitlement;

    @Column(name = "units_in_kind", nullable = false, updatable = false)
    private long unitsInKind;

    @Column(name = "fractional_remainder", nullable = false, updatable = false, precision = 24, scale = 6)
    private BigDecimal fractionalRemainder;

    /** The reference-price SNAPSHOT that valued the fractional remainder — recorded immutably. */
    @Column(name = "cash_rate", nullable = false, updatable = false, precision = 19, scale = 6)
    private BigDecimal cashRate;

    @Column(name = "cash_value", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal cashValue;

    @Column(name = "allocated_at", nullable = false, updatable = false)
    private Instant allocatedAt;

    protected CashInLieuAllocation() {}

    public CashInLieuAllocation(UUID id, String subjectRef, String eventRef, BigDecimal rawEntitlement,
                                long unitsInKind, BigDecimal fractionalRemainder, BigDecimal cashRate,
                                BigDecimal cashValue, Instant allocatedAt) {
        this.id = id;
        this.subjectRef = subjectRef;
        this.eventRef = eventRef;
        this.rawEntitlement = rawEntitlement;
        this.unitsInKind = unitsInKind;
        this.fractionalRemainder = fractionalRemainder;
        this.cashRate = cashRate;
        this.cashValue = cashValue;
        this.allocatedAt = allocatedAt;
    }

    public UUID getId() { return id; }
    public String getSubjectRef() { return subjectRef; }
    public String getEventRef() { return eventRef; }
    public BigDecimal getRawEntitlement() { return rawEntitlement; }
    public long getUnitsInKind() { return unitsInKind; }
    public BigDecimal getFractionalRemainder() { return fractionalRemainder; }
    public BigDecimal getCashRate() { return cashRate; }
    public BigDecimal getCashValue() { return cashValue; }
    public Instant getAllocatedAt() { return allocatedAt; }
}
