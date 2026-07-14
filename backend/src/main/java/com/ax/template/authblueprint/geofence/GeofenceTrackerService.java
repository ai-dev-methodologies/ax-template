package com.ax.template.authblueprint.geofence;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * geofence-transition-l0 sole orchestrator — a raw containment observation is NOT immediately a
 * transition. EVENT-TIME driven throughout: every decision uses the caller-supplied observedAt,
 * never the wall clock (GEOFENCE-DWELL-001 determinism). A raw observation that differs from the
 * tracker's confirmed state starts (or continues) a PENDING confirmation, recording the first such
 * observation's timestamp; it commits (flips confirmed state + writes an immutable
 * {@link GeofenceTransition}) only once a later same-direction observation arrives with the dwell
 * threshold elapsed and no intervening reversal. A raw observation matching the ALREADY-confirmed
 * state cancels any pending confirmation outright (GEOFENCE-FLAP-SUPPRESS-001) — flapping within
 * the dwell window commits zero transitions. {@link GeofenceTransition} rows are members:
 * {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class GeofenceTrackerService {

    /** Policy constant — a fork-receiver swap point (per-zone dwell is a natural future extension). */
    static final long DEFAULT_DWELL_SECONDS = 60L;

    private final GeofenceTrackerRepository trackers;
    private final MemberWriter members;
    private final GeofenceMetrics metrics;
    private final Clock clock;

    public GeofenceTrackerService(GeofenceTrackerRepository trackers, MemberWriter members,
                                  GeofenceMetrics metrics, Clock clock) {
        this.trackers = trackers;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** Register a tracker for a (subjectId, zoneId) pair — starts OUTSIDE, no pending. */
    @Transactional
    public GeofenceTracker register(String subjectId, String zoneId) {
        if (subjectId == null || subjectId.isBlank() || zoneId == null || zoneId.isBlank()) {
            metrics.record("register", "invalid");
            throw GeofenceException.invalidInput("subjectId and zoneId are required");
        }
        try {
            GeofenceTracker saved = trackers.saveAndFlush(
                new GeofenceTracker(UUID.randomUUID(), subjectId, zoneId, Instant.now(clock)));
            metrics.record("register", "ok");
            return saved;
        } catch (DataIntegrityViolationException dup) {
            metrics.record("register", "invalid");
            throw GeofenceException.invalidInput(
                "a tracker already exists for subject=" + subjectId + " zone=" + zoneId);
        }
    }

    /**
     * GEOFENCE-DWELL/FLAP-SUPPRESS-001 — process one event-time raw observation. See class javadoc
     * for the full debounce/hysteresis decision table.
     */
    @Transactional
    public GeofenceTracker observe(UUID trackerId, Presence rawState, Instant observedAt) {
        GeofenceTracker t = trackers.findById(trackerId).orElseThrow(GeofenceException::notFound);

        if (rawState == t.getConfirmedState()) {
            if (t.hasPending()) {
                t.clearPending();                                    // GEOFENCE-FLAP-SUPPRESS-001
                metrics.record("observe", "pending_cancelled");
            } else {
                metrics.record("observe", "no_op");
            }
            return t;
        }

        TransitionDirection implied = rawState == Presence.INSIDE ? TransitionDirection.ENTER : TransitionDirection.EXIT;
        if (!t.hasPending() || t.getPendingDirection() != implied) {
            t.startPending(implied, observedAt);                      // GEOFENCE-DWELL-001 — (re)start the window
            metrics.record("observe", "pending_started");
            return t;
        }

        long elapsedSeconds = Duration.between(t.getPendingSince(), observedAt).getSeconds();
        if (elapsedSeconds >= DEFAULT_DWELL_SECONDS) {
            GeofenceTransition tr = new GeofenceTransition(UUID.randomUUID(), t.getId(), t.getZoneId(),
                implied, t.getPendingSince(), observedAt, Instant.now(clock));
            members.persistAndFlush(tr);
            t.confirmTransition(rawState);                            // GEOFENCE-DWELL-001 — confirmed
            metrics.record("observe", "confirmed");
        } else {
            metrics.record("observe", "no_op");                       // still within the dwell window
        }
        return t;
    }

    @Transactional(readOnly = true)
    public GeofenceTracker get(UUID trackerId) {
        return trackers.findById(trackerId).orElseThrow(GeofenceException::notFound);
    }

    @Transactional(readOnly = true)
    public List<GeofenceTransition> transitions(UUID trackerId) {
        get(trackerId);                                               // 404 before an empty list
        return trackers.findTransitionsByTrackerId(trackerId, PageRequest.of(0, 500));
    }
}
