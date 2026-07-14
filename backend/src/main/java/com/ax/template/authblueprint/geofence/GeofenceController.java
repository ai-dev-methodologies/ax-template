package com.ax.template.authblueprint.geofence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * geofence-transition-l0 thin controller. Delegates to {@link GeofenceTrackerService}. Every
 * observation carries its own event-time timestamp — the controller never substitutes the wall
 * clock (GEOFENCE-DWELL-001 determinism).
 */
@RestController
public class GeofenceController {

    public record RegisterReq(@NotBlank @Size(max = 200) String subjectId,
                              @NotBlank @Size(max = 200) String zoneId) {}
    public record ObserveReq(@NotNull Presence rawState, @NotNull Instant observedAt) {}

    public record TrackerDto(UUID id, String subjectId, String zoneId, Presence confirmedState,
                             TransitionDirection pendingDirection, Instant pendingSince) {
        static TrackerDto of(GeofenceTracker t) {
            return new TrackerDto(t.getId(), t.getSubjectId(), t.getZoneId(), t.getConfirmedState(),
                t.getPendingDirection(), t.getPendingSince());
        }
    }

    public record TransitionDto(UUID id, String zoneId, TransitionDirection direction,
                                Instant observedAt, Instant confirmedAt) {
        static TransitionDto of(GeofenceTransition t) {
            return new TransitionDto(t.getId(), t.getZoneId(), t.getDirection(), t.getObservedAt(), t.getConfirmedAt());
        }
    }

    private final GeofenceTrackerService service;

    public GeofenceController(GeofenceTrackerService service) {
        this.service = service;
    }

    @PostMapping("/api/geofence/trackers")
    public ResponseEntity<TrackerDto> register(@Valid @RequestBody RegisterReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TrackerDto.of(service.register(req.subjectId(), req.zoneId())));
    }

    /** GEOFENCE-DWELL/FLAP-SUPPRESS-001 — one event-time raw observation. */
    @PostMapping("/api/geofence/trackers/{id}/observations")
    public TrackerDto observe(@PathVariable UUID id, @Valid @RequestBody ObserveReq req) {
        return TrackerDto.of(service.observe(id, req.rawState(), req.observedAt()));
    }

    @GetMapping("/api/geofence/trackers/{id}")
    public TrackerDto get(@PathVariable UUID id) {
        return TrackerDto.of(service.get(id));
    }

    /** GEOFENCE-CONFIRM-001 — the confirmed, immutable, dual-timestamped transition history. */
    @GetMapping("/api/geofence/trackers/{id}/transitions")
    public List<TransitionDto> transitions(@PathVariable UUID id) {
        return service.transitions(id).stream().map(TransitionDto::of).toList();
    }

    @ExceptionHandler(GeofenceException.class)
    public ResponseEntity<ProblemDetail> handle(GeofenceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
