package com.ax.template.authblueprint.geofence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * geofence-transition-l0 root: one (subject, zone) tracker. Carries the CONFIRMED containment
 * state plus, while a raw signal differs from it, a PENDING direction + the timestamp the pending
 * confirmation started (pendingSince) — both event-time values, never a wall-clock read
 * (GEOFENCE-DWELL-001). A raw observation matching the confirmed state cancels any pending
 * confirmation outright (GEOFENCE-FLAP-SUPPRESS-001) rather than pausing it — a later same-
 * direction observation starts a FRESH pending window. Mutated only by
 * {@link GeofenceTrackerService} (no public setter); confirmed transitions are recorded as
 * {@link GeofenceTransition} members.
 */
@AggregateRoot
@Entity
@Table(name = "geofence_trackers", uniqueConstraints = {
    @UniqueConstraint(name = "uq_geofence_tracker_subject_zone", columnNames = {"subject_id", "zone_id"})
})
public class GeofenceTracker {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_id", nullable = false, updatable = false, length = 200)
    private String subjectId;

    @Column(name = "zone_id", nullable = false, updatable = false, length = 200)
    private String zoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmed_state", nullable = false, length = 16)
    private Presence confirmedState;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_direction", length = 16)
    private TransitionDirection pendingDirection;

    /** Event-time — the FIRST raw observation that started the current pending confirmation. */
    @Column(name = "pending_since")
    private Instant pendingSince;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GeofenceTracker() {}

    public GeofenceTracker(UUID id, String subjectId, String zoneId, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.zoneId = zoneId;
        this.confirmedState = Presence.OUTSIDE;      // every tracker starts OUTSIDE
        this.createdAt = createdAt;
    }

    /** GEOFENCE-DWELL-001 — (re)start a pending confirmation toward {@code direction} at {@code at}. */
    void startPending(TransitionDirection direction, Instant at) {
        this.pendingDirection = direction;
        this.pendingSince = at;
    }

    /** GEOFENCE-FLAP-SUPPRESS-001 — cancel (not pause) the pending confirmation. */
    void clearPending() {
        this.pendingDirection = null;
        this.pendingSince = null;
    }

    /** GEOFENCE-DWELL-001 — the dwell threshold was reached: flip confirmed state, clear pending. */
    void confirmTransition(Presence newConfirmedState) {
        this.confirmedState = newConfirmedState;
        clearPending();
    }

    public boolean hasPending() {
        return pendingDirection != null;
    }

    public UUID getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public String getZoneId() { return zoneId; }
    public Presence getConfirmedState() { return confirmedState; }
    public TransitionDirection getPendingDirection() { return pendingDirection; }
    public Instant getPendingSince() { return pendingSince; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
