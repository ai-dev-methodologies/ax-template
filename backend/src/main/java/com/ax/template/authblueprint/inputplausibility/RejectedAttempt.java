package com.ax.template.authblueprint.inputplausibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One RECORDED rejected submission (PLAUSIBILITY-REJECT-001). Immutable and append-only — an
 * implausible self-report is never silently dropped: the channel, the reported value, the
 * deterministic {@link RejectReason} (IMPLAUSIBLE_RANGE / IMPLAUSIBLE_RATE), the prior-value /
 * elapsed / computed-rate basis (where the rate gate applied), the actor, and the instant are
 * recorded BEFORE the 422 is returned, so the rejection is auditable (fraud signal, calibration).
 * A rejected attempt NEVER becomes the channel's new prior — the accepted-reading state is untouched.
 */
@AggregateMember(root = PlausibilityChannel.class)
@Entity
@Table(name = "plausibility_rejected_attempts")
public class RejectedAttempt {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private UUID channelId;

    @Column(name = "reported_value", nullable = false, updatable = false, precision = 38, scale = 9)
    private BigDecimal reportedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = 40)
    private RejectReason reason;

    /** The prior accepted value at rejection time (null when the channel had no prior). */
    @Column(name = "prior_value", updatable = false, precision = 38, scale = 9)
    private BigDecimal priorValue;

    @Column(name = "elapsed_seconds", nullable = false, updatable = false)
    private long elapsedSeconds;

    /** The computed |delta| per second that breached the limit (null for a pure range rejection). */
    @Column(name = "computed_rate", updatable = false, precision = 38, scale = 9)
    private BigDecimal computedRate;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected RejectedAttempt() {}

    public RejectedAttempt(UUID id, UUID channelId, BigDecimal reportedValue, RejectReason reason,
                           BigDecimal priorValue, long elapsedSeconds, BigDecimal computedRate,
                           String actor, Instant occurredAt) {
        this.id = id;
        this.channelId = channelId;
        this.reportedValue = reportedValue;
        this.reason = reason;
        this.priorValue = priorValue;
        this.elapsedSeconds = elapsedSeconds;
        this.computedRate = computedRate;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getChannelId() { return channelId; }
    public BigDecimal getReportedValue() { return reportedValue; }
    public RejectReason getReason() { return reason; }
    public BigDecimal getPriorValue() { return priorValue; }
    public long getElapsedSeconds() { return elapsedSeconds; }
    public BigDecimal getComputedRate() { return computedRate; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
