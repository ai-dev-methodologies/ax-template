package com.ax.template.authblueprint.inputplausibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One ACCEPTED self-reported reading (PLAUSIBILITY-PROVENANCE-001). Immutable and append-only. It
 * persists not just the value but its UNVERIFIED provenance ({@link VerificationStatus} — only ever
 * SELF_REPORTED_UNVERIFIED) and the full plausibility BASIS on which it was admitted: which checks
 * ran ({@code checksRan}, e.g. "RANGE" or "RANGE,RATE"), whether a prior existed and its value
 * ({@code hadPrior} / {@code priorValue}), the elapsed seconds since the prior, and the computed
 * rate. A downstream consumer therefore always sees the value's origin and the basis for its
 * admission — a bare value with no status and no basis is unrepresentable.
 */
@AggregateMember(root = PlausibilityChannel.class)
@Entity
@Table(name = "plausibility_readings")
@Check(constraints = "verification_status = 'SELF_REPORTED_UNVERIFIED' AND elapsed_seconds >= 0")
public class PlausibilityReading {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private UUID channelId;

    @Column(name = "reported_value", nullable = false, updatable = false, precision = 38, scale = 9)
    private BigDecimal reportedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, updatable = false, length = 40)
    private VerificationStatus verificationStatus;

    /** The plausibility checks that ran for this reading, e.g. "RANGE" or "RANGE,RATE". */
    @Column(name = "checks_ran", nullable = false, updatable = false, length = 40)
    private String checksRan;

    @Column(name = "had_prior", nullable = false, updatable = false)
    private boolean hadPrior;

    /** The prior accepted value the rate gate used (null when this was the first reading). */
    @Column(name = "prior_value", updatable = false, precision = 38, scale = 9)
    private BigDecimal priorValue;

    @Column(name = "elapsed_seconds", nullable = false, updatable = false)
    private long elapsedSeconds;

    /** The computed |delta| per second (null when there was no prior to compute it against). */
    @Column(name = "computed_rate", updatable = false, precision = 38, scale = 9)
    private BigDecimal computedRate;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected PlausibilityReading() {}

    public PlausibilityReading(UUID id, UUID channelId, BigDecimal reportedValue, String checksRan,
                               boolean hadPrior, BigDecimal priorValue, long elapsedSeconds,
                               BigDecimal computedRate, String actor, Instant occurredAt) {
        this.id = id;
        this.channelId = channelId;
        this.reportedValue = reportedValue;
        this.verificationStatus = VerificationStatus.SELF_REPORTED_UNVERIFIED;
        this.checksRan = checksRan;
        this.hadPrior = hadPrior;
        this.priorValue = priorValue;
        this.elapsedSeconds = elapsedSeconds;
        this.computedRate = computedRate;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getChannelId() { return channelId; }
    public BigDecimal getReportedValue() { return reportedValue; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getChecksRan() { return checksRan; }
    public boolean isHadPrior() { return hadPrior; }
    public BigDecimal getPriorValue() { return priorValue; }
    public long getElapsedSeconds() { return elapsedSeconds; }
    public BigDecimal getComputedRate() { return computedRate; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
