package com.ax.template.authblueprint.eventingest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * monotonic-event-ingest-l0 thin controller — the transport-agnostic ingest surface (a webhook
 * POST in the reference workload; a queue consumer or Kafka record in a fork). Delegates to
 * {@link EventIngestService}. NOTE: {@code ApplyEventReq} deliberately carries NO
 * {@code recordedAt} field — the server clock is the sole source (INGEST-CAPTURE-001).
 */
@RestController
public class EventIngestController {

    public record ApplyEventReq(@NotNull IngestStream stream,
                                @NotBlank @Size(max = 200) String subjectId,
                                @NotBlank @Size(max = 200) String eventId,
                                @NotNull Instant occurredAt,
                                @NotNull Instant capturedAt,
                                @NotBlank @Size(max = 200) String stateValue) {}

    public record IngestStateDto(UUID id, String stream, String subjectId, String stateValue,
                                 String lastAppliedEventId, Instant lastAppliedOccurredAt,
                                 Instant lastAppliedCapturedAt, Instant lastAppliedRecordedAt) {
        static IngestStateDto of(IngestState s) {
            return new IngestStateDto(s.getId(), s.getStream(), s.getSubjectId(), s.getStateValue(),
                s.getLastAppliedEventId(), s.getLastAppliedOccurredAt(), s.getLastAppliedCapturedAt(),
                s.getLastAppliedRecordedAt());
        }
    }

    private final EventIngestService service;

    public EventIngestController(EventIngestService service) {
        this.service = service;
    }

    /**
     * Always 200 — this is a transport ACK, not a resource-creation response: an applied
     * event, a stale-dropped event, and a duplicate-dropped event are ALL acknowledged
     * identically so the provider never redelivers a successfully-received event forever.
     */
    @PostMapping("/api/event-ingest/events")
    public ResponseEntity<IngestStateDto> apply(@Valid @RequestBody ApplyEventReq req) {
        IngestState row = service.apply(req.stream(), req.subjectId(), req.eventId(),
            req.occurredAt(), req.capturedAt(), req.stateValue());
        return ResponseEntity.status(HttpStatus.OK).body(IngestStateDto.of(row));
    }

    @GetMapping("/api/event-ingest/streams/{stream}/subjects/{subjectId}")
    public IngestStateDto get(@PathVariable IngestStream stream, @PathVariable String subjectId) {
        return IngestStateDto.of(service.get(stream, subjectId));
    }

    @ExceptionHandler(EventIngestException.class)
    public ResponseEntity<ProblemDetail> handle(EventIngestException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
