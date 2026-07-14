package com.ax.template.authblueprint.intervalexclusivity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One booking of a {@link BookingResource} for the half-open interval {@code [startAt, endAt)}
 * (IVX-OVERLAP-001). Unlike most members in this catalog, a booking's interval IS mutable while
 * ACTIVE (IVX-MUTATE-003): {@link #resize} is called directly by the service (mirrors
 * {@code ThresholdRegister#advanceAnchor}), always under the resource's row lock; {@code status} is
 * mutated ONLY by {@link BookingStateMachine} (the one-way {@code ACTIVE -> CANCELLED} edge — sole
 * mutator). No public setter anywhere.
 */
@AggregateMember(root = BookingResource.class)
@Entity
@Table(name = "bookings")
@Check(constraints = "start_at < end_at")
public class Booking {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "resource_key", nullable = false, updatable = false, length = 200)
    private String resourceKey;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Booking() {}

    public Booking(UUID id, String resourceKey, Instant startAt, Instant endAt, Instant createdAt) {
        this.id = id;
        this.resourceKey = resourceKey;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = BookingStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook (service only, under the resource row lock) — IVX-MUTATE-003. */
    void resize(Instant newStart, Instant newEnd) {
        this.startAt = newStart;
        this.endAt = newEnd;
    }

    /** Sole-mutator hook ({@link BookingStateMachine} only) — the one-way cancel edge. */
    void markCancelled() {
        this.status = BookingStatus.CANCELLED;
    }

    public UUID getId() { return id; }
    public String getResourceKey() { return resourceKey; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public BookingStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
