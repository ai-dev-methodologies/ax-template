package com.ax.template.authblueprint.inputplausibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One ACCEPTED DATE-typed asserted fact (PLAUSIBILITY-DATE-RANGE-001). Immutable and
 * append-only, admitted ONLY as {@link VerificationStatus#SELF_REPORTED_UNVERIFIED} — the
 * same unverified-provenance contract as the numeric {@link PlausibilityReading}, plus the
 * reference instant the window was evaluated against (never re-derivable from wall-clock).
 */
@AggregateMember(root = DatePlausibilityChannel.class)
@Entity
@Table(name = "date_plausibility_readings")
@Check(constraints = "verification_status = 'SELF_REPORTED_UNVERIFIED'")
public class DatePlausibilityReading {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private UUID channelId;

    @Column(name = "asserted_at", nullable = false, updatable = false)
    private Instant assertedAt;

    /** The reference instant (injected Clock, never wall-clock) the window was evaluated against. */
    @Column(name = "reference_at", nullable = false, updatable = false)
    private Instant referenceAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, updatable = false, length = 40)
    private VerificationStatus verificationStatus;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected DatePlausibilityReading() {}

    public DatePlausibilityReading(UUID id, UUID channelId, Instant assertedAt, Instant referenceAt,
                                   String actor, Instant occurredAt) {
        this.id = id;
        this.channelId = channelId;
        this.assertedAt = assertedAt;
        this.referenceAt = referenceAt;
        this.verificationStatus = VerificationStatus.SELF_REPORTED_UNVERIFIED;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getChannelId() { return channelId; }
    public Instant getAssertedAt() { return assertedAt; }
    public Instant getReferenceAt() { return referenceAt; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
