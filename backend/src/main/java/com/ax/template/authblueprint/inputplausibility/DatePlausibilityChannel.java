package com.ax.template.authblueprint.inputplausibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * PLAUSIBILITY-DATE-RANGE/FUTURE-001 root: the plausible window config for a DATE-typed
 * self-reported asserted fact (a claimed event date — e.g. a loss date), distinct from the
 * numeric {@link PlausibilityChannel} (no rate-of-change concept applies to a single asserted
 * date). A submission is validated against a HALF-OPEN-in-spirit window relative to the
 * reference instant (the injected {@link java.time.Clock}'s now, never wall-clock):
 * [reference - maxLookback, reference + maxLookahead]. Both bounds are configured durations
 * (seconds), immutable once the channel is defined.
 */
@AggregateRoot
@Entity
@Table(name = "date_plausibility_channels")
@Check(constraints = "max_lookback_seconds >= 0 AND max_lookahead_seconds >= 0")
public class DatePlausibilityChannel {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** What this channel reports on (opaque label, recorded verbatim) — immutable. */
    @Column(name = "subject_ref", nullable = false, updatable = false, length = 200)
    private String subjectRef;

    /** PLAUSIBILITY-DATE-RANGE-001 — how far in the past an asserted date may lie, in seconds. */
    @Column(name = "max_lookback_seconds", nullable = false, updatable = false)
    private long maxLookbackSeconds;

    /** PLAUSIBILITY-DATE-FUTURE-001 — how far in the future an asserted date may lie, in seconds. */
    @Column(name = "max_lookahead_seconds", nullable = false, updatable = false)
    private long maxLookaheadSeconds;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DatePlausibilityChannel() {}

    public DatePlausibilityChannel(UUID id, String subjectRef, long maxLookbackSeconds,
                                   long maxLookaheadSeconds, Instant createdAt) {
        this.id = id;
        this.subjectRef = subjectRef;
        this.maxLookbackSeconds = maxLookbackSeconds;
        this.maxLookaheadSeconds = maxLookaheadSeconds;
        this.createdAt = createdAt;
    }

    /** PLAUSIBILITY-DATE-RANGE/FUTURE-001 — true when {@code assertedAt} lies within
     *  [referenceAt - maxLookback, referenceAt + maxLookahead], both bounds INCLUSIVE. */
    public boolean inWindow(Instant assertedAt, Instant referenceAt) {
        Instant earliest = referenceAt.minusSeconds(maxLookbackSeconds);
        Instant latest = referenceAt.plusSeconds(maxLookaheadSeconds);
        return !assertedAt.isBefore(earliest) && !assertedAt.isAfter(latest);
    }

    public UUID getId() { return id; }
    public String getSubjectRef() { return subjectRef; }
    public long getMaxLookbackSeconds() { return maxLookbackSeconds; }
    public long getMaxLookaheadSeconds() { return maxLookaheadSeconds; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
