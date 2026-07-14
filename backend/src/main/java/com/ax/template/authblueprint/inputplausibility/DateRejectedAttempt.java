package com.ax.template.authblueprint.inputplausibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One RECORDED rejected DATE-typed submission — same reject/flag semantics as the numeric
 * {@link RejectedAttempt}: an implausible asserted date is never silently dropped. Immutable
 * and append-only.
 */
@AggregateMember(root = DatePlausibilityChannel.class)
@Entity
@Table(name = "date_plausibility_rejected_attempts")
public class DateRejectedAttempt {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private UUID channelId;

    @Column(name = "asserted_at", nullable = false, updatable = false)
    private Instant assertedAt;

    @Column(name = "reference_at", nullable = false, updatable = false)
    private Instant referenceAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = 40)
    private DateRejectReason reason;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected DateRejectedAttempt() {}

    public DateRejectedAttempt(UUID id, UUID channelId, Instant assertedAt, Instant referenceAt,
                               DateRejectReason reason, String actor, Instant occurredAt) {
        this.id = id;
        this.channelId = channelId;
        this.assertedAt = assertedAt;
        this.referenceAt = referenceAt;
        this.reason = reason;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getChannelId() { return channelId; }
    public Instant getAssertedAt() { return assertedAt; }
    public Instant getReferenceAt() { return referenceAt; }
    public DateRejectReason getReason() { return reason; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
