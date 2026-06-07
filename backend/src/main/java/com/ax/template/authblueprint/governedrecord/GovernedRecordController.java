package com.ax.template.authblueprint.governedrecord;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * attested-change-record-l0 thin controller. Any authenticated user may create/edit a governed datum;
 * the edit MUST carry a reason. NOTE: {@code reason} is deliberately NOT @NotBlank here — the service
 * rejects a blank reason with 422 ATTESTED_REASON_REQUIRED (the domain contract), not a 400.
 * Delegates to {@link GovernedRecordService} ONLY.
 */
@RestController
public class GovernedRecordController {

    public record CreateReq(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 2000) String value) {}
    public record ChangeReq(@NotBlank @Size(max = 2000) String newValue, @Size(max = 1000) String reason) {}

    public record DatumDto(UUID id, String name, String value, Long version) {
        static DatumDto of(GovernedDatum d) {
            return new DatumDto(d.getId(), d.getName(), d.getValue(), d.getVersion());
        }
    }
    public record ChangeDto(long sequenceNo, String oldValue, String newValue, String reason,
                            String reasonVocabVersion, String actor, Instant occurredAt) {
        static ChangeDto of(ChangeRecord c) {
            return new ChangeDto(c.getSequenceNo(), c.getOldValue(), c.getNewValue(), c.getReason(),
                c.getReasonVocabVersion(), c.getActor(), c.getOccurredAt());
        }
    }

    private final GovernedRecordService service;

    public GovernedRecordController(GovernedRecordService service) {
        this.service = service;
    }

    @PostMapping("/api/governed-data")
    public ResponseEntity<DatumDto> create(@Valid @RequestBody CreateReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DatumDto.of(service.createDatum(auth.getName(), req.name(), req.value())));
    }

    @PutMapping("/api/governed-data/{id}")
    public DatumDto change(@PathVariable UUID id, @Valid @RequestBody ChangeReq req, Authentication auth) {
        return DatumDto.of(service.changeValue(id, req.newValue(), req.reason(), auth.getName()));
    }

    @GetMapping("/api/governed-data/{id}")
    public DatumDto get(@PathVariable UUID id) {
        return DatumDto.of(service.get(id));
    }

    @GetMapping("/api/governed-data/{id}/history")
    public PageEnvelope<ChangeDto> history(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.history(id, page, size), ChangeDto::of);
    }

    @ExceptionHandler(AttestedException.class)
    public ResponseEntity<ProblemDetail> handle(AttestedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
