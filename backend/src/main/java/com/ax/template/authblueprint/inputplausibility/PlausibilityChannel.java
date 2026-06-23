package com.ax.template.authblueprint.inputplausibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * self-reported-input-plausibility-l0 root: one kind of SELF-REPORTED, server-unverifiable value
 * (a claimed location coordinate, a self-meter-read, an odometer entry, an attestation count). The
 * channel pins the immutable PLAUSIBILITY CONFIG — the plausible range [{@code minValue},
 * {@code maxValue}] (PLAUSIBILITY-RANGE-001) and the max delta-per-second rate ceiling
 * (PLAUSIBILITY-RATE-001) — and carries the mutable PRIOR-ACCEPTED-READING pointer
 * ({@code priorValue} + {@code priorAt}) the rate gate evaluates against. The prior pointer moves
 * ONLY via the package-private {@link #recordAccepted} hook, called by {@link PlausibilityService}
 * under the channel's PESSIMISTIC_WRITE row lock so the read-prior / append-reading sequence is
 * atomic (PLAUSIBILITY-CONCURRENT-001). Accepted {@link PlausibilityReading}s and rejected
 * {@link RejectedAttempt}s are members written through the root.
 */
@AggregateRoot
@Entity
@Table(name = "plausibility_channels")
@Check(constraints =
    "min_value <= max_value AND max_delta_per_second >= 0"
    + " AND (prior_value IS NULL) = (prior_at IS NULL)")
public class PlausibilityChannel {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** What this channel reports on (opaque label, recorded verbatim) — immutable. */
    @Column(name = "subject_ref", nullable = false, updatable = false, length = 200)
    private String subjectRef;

    /** PLAUSIBILITY-RANGE-001 — configured plausible lower bound (inclusive); immutable config. */
    @Column(name = "min_value", nullable = false, updatable = false, precision = 38, scale = 9)
    private BigDecimal minValue;

    /** PLAUSIBILITY-RANGE-001 — configured plausible upper bound (inclusive); immutable config. */
    @Column(name = "max_value", nullable = false, updatable = false, precision = 38, scale = 9)
    private BigDecimal maxValue;

    /** PLAUSIBILITY-RATE-001 — configured max |delta| per elapsed second; immutable config. */
    @Column(name = "max_delta_per_second", nullable = false, updatable = false, precision = 38, scale = 9)
    private BigDecimal maxDeltaPerSecond;

    /** The prior ACCEPTED value the next rate check evaluates against — null until the first reading. */
    @Column(name = "prior_value", precision = 38, scale = 9)
    private BigDecimal priorValue;

    /** The recorded instant of the prior accepted reading — null until the first reading. */
    @Column(name = "prior_at")
    private Instant priorAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlausibilityChannel() {}

    public PlausibilityChannel(UUID id, String subjectRef, BigDecimal minValue, BigDecimal maxValue,
                               BigDecimal maxDeltaPerSecond, Instant createdAt) {
        this.id = id;
        this.subjectRef = subjectRef;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.maxDeltaPerSecond = maxDeltaPerSecond;
        this.createdAt = createdAt;
    }

    /** PLAUSIBILITY-RANGE-001 — true when {@code value} lies within the configured [min, max]. */
    public boolean inRange(BigDecimal value) {
        return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) <= 0;
    }

    public boolean hasPrior() {
        return priorValue != null;
    }

    /** Sole-mutator hook — advance the prior-accepted pointer when a reading is accepted
     *  (PLAUSIBILITY-PROVENANCE/CONCURRENT-001). A rejected attempt NEVER calls this. */
    void recordAccepted(BigDecimal acceptedValue, Instant acceptedAt) {
        this.priorValue = acceptedValue;
        this.priorAt = acceptedAt;
    }

    public UUID getId() { return id; }
    public String getSubjectRef() { return subjectRef; }
    public BigDecimal getMinValue() { return minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public BigDecimal getMaxDeltaPerSecond() { return maxDeltaPerSecond; }
    public BigDecimal getPriorValue() { return priorValue; }
    public Instant getPriorAt() { return priorAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
