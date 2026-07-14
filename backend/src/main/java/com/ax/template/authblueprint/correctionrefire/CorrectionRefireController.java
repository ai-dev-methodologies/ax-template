package com.ax.template.authblueprint.correctionrefire;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import java.util.List;

/**
 * correction-refire-l0 thin controller. Delegates to {@link CorrectionRefireService}.
 */
@RestController
public class CorrectionRefireController {

    public record PublishReq(@NotBlank @Size(max = 4000) String content) {}

    public record RecordDto(String subjectRef, int version, String content, String contentHash,
                            Integer correctsVersion, Instant publishedAt) {
        static RecordDto of(CorrectedRecord r) {
            return new RecordDto(r.getSubjectRef(), r.getVersion(), r.getContent(), r.getContentHash(),
                r.getCorrectsVersion(), r.getPublishedAt());
        }
    }

    public record AckDto(int version, AckStatus status, Instant createdAt, Instant closedAt) {
        static AckDto of(int version, AckRecord a) {
            return new AckDto(version, a.getStatus(), a.getCreatedAt(), a.getClosedAt());
        }
    }

    private final CorrectionRefireService service;

    public CorrectionRefireController(CorrectionRefireService service) {
        this.service = service;
    }

    /** CRF-SUPERSEDE-001 / CRF-IDEMPOTENT-003 — publish the first version, or a correction. */
    @PostMapping("/api/correction-refire/subjects/{subjectRef}/publish")
    public ResponseEntity<RecordDto> publish(@PathVariable String subjectRef, @Valid @RequestBody PublishReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RecordDto.of(service.publish(subjectRef, req.content())));
    }

    @PostMapping("/api/correction-refire/subjects/{subjectRef}/versions/{version}/ack")
    public AckDto acknowledge(@PathVariable String subjectRef, @PathVariable int version) {
        return AckDto.of(version, service.acknowledge(subjectRef, version));
    }

    /** CRF-CHAIN-004 — always derived (MAX version), never a stored pointer. */
    @GetMapping("/api/correction-refire/subjects/{subjectRef}/current")
    public RecordDto current(@PathVariable String subjectRef) {
        return RecordDto.of(service.current(subjectRef));
    }

    @GetMapping("/api/correction-refire/subjects/{subjectRef}/versions/{version}")
    public RecordDto getVersion(@PathVariable String subjectRef, @PathVariable int version) {
        return RecordDto.of(service.getVersionOrThrow(subjectRef, version));
    }

    @GetMapping("/api/correction-refire/subjects/{subjectRef}/versions/{version}/ack")
    public AckDto getAck(@PathVariable String subjectRef, @PathVariable int version) {
        return AckDto.of(version, service.getAck(subjectRef, version));
    }

    @GetMapping("/api/correction-refire/subjects/{subjectRef}/versions")
    public List<RecordDto> chain(@PathVariable String subjectRef) {
        return service.chain(subjectRef).stream().map(RecordDto::of).toList();
    }

    @ExceptionHandler(CorrectionRefireException.class)
    public ResponseEntity<ProblemDetail> handle(CorrectionRefireException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
