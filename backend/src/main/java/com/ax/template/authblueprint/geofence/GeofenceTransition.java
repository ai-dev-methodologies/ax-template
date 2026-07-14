package com.ax.template.authblueprint.geofence;

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
 * geofence-transition-l0 member: one CONFIRMED, immutable transition (GEOFENCE-CONFIRM-001). Carries
 * BOTH timestamps — observedAt (the first raw observation that started the pending confirmation) and
 * confirmedAt (the observation whose arrival satisfied the dwell threshold) — the dual-timestamp
 * audit record of the debounce discipline itself. No public setter, no delete path; a member of
 * {@link GeofenceTracker} written only through {@link GeofenceTrackerService} via
 * {@code common/MemberWriter} (HG-AGG-REPO — no repository of its own).
 */
@AggregateMember(root = GeofenceTracker.class)
@Entity
@Table(name = "geofence_transitions")
@Check(constraints = "confirmed_at >= observed_at")
public class GeofenceTransition {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tracker_id", nullable = false, updatable = false)
    private UUID trackerId;

    @Column(name = "zone_id", nullable = false, updatable = false, length = 200)
    private String zoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, updatable = false, length = 16)
    private TransitionDirection direction;

    @Column(name = "observed_at", nullable = false, updatable = false)
    private Instant observedAt;

    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GeofenceTransition() {}

    public GeofenceTransition(UUID id, UUID trackerId, String zoneId, TransitionDirection direction,
                              Instant observedAt, Instant confirmedAt, Instant createdAt) {
        this.id = id;
        this.trackerId = trackerId;
        this.zoneId = zoneId;
        this.direction = direction;
        this.observedAt = observedAt;
        this.confirmedAt = confirmedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getTrackerId() { return trackerId; }
    public String getZoneId() { return zoneId; }
    public TransitionDirection getDirection() { return direction; }
    public Instant getObservedAt() { return observedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
