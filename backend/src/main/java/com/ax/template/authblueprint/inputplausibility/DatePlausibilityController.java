package com.ax.template.authblueprint.inputplausibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PLAUSIBILITY-DATE-RANGE/FUTURE-001 thin controller. The acting principal is ALWAYS the
 * authenticated caller. Delegates to {@link DatePlausibilityService}. Shares the
 * {@code /api/input-plausibility/**} authorization prefix with {@link PlausibilityController}.
 */
@RestController
public class DatePlausibilityController {

    public record DefineReq(@NotBlank @Size(max = 200) String subjectRef,
                            @NotNull Long maxLookbackSeconds,
                            @NotNull Long maxLookaheadSeconds) {}
    public record SubmitReq(@NotNull Instant assertedAt) {}

    public record DateChannelDto(UUID id, String subjectRef, long maxLookbackSeconds,
                                 long maxLookaheadSeconds, Long version) {
        static DateChannelDto of(DatePlausibilityChannel c) {
            return new DateChannelDto(c.getId(), c.getSubjectRef(), c.getMaxLookbackSeconds(),
                c.getMaxLookaheadSeconds(), c.getVersion());
        }
    }
    public record DateReadingDto(UUID id, Instant assertedAt, Instant referenceAt,
                                 VerificationStatus verificationStatus, String actor, Instant occurredAt) {
        static DateReadingDto of(DatePlausibilityReading r) {
            return new DateReadingDto(r.getId(), r.getAssertedAt(), r.getReferenceAt(),
                r.getVerificationStatus(), r.getActor(), r.getOccurredAt());
        }
    }
    public record DateRejectedAttemptDto(UUID id, Instant assertedAt, Instant referenceAt,
                                         DateRejectReason reason, String actor, Instant occurredAt) {
        static DateRejectedAttemptDto of(DateRejectedAttempt a) {
            return new DateRejectedAttemptDto(a.getId(), a.getAssertedAt(), a.getReferenceAt(),
                a.getReason(), a.getActor(), a.getOccurredAt());
        }
    }

    private final DatePlausibilityService service;

    public DatePlausibilityController(DatePlausibilityService service) {
        this.service = service;
    }

    @PostMapping("/api/input-plausibility/date-channels")
    public ResponseEntity<DateChannelDto> define(@Valid @RequestBody DefineReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DateChannelDto.of(
            service.define(req.subjectRef(), req.maxLookbackSeconds(), req.maxLookaheadSeconds())));
    }

    /** PLAUSIBILITY-DATE-RANGE/FUTURE-001 — plausibility-gate one DATE-typed submission. */
    @PostMapping("/api/input-plausibility/date-channels/{id}/submissions")
    public ResponseEntity<DateReadingDto> submit(@PathVariable UUID id, @Valid @RequestBody SubmitReq req,
                                                 Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DateReadingDto.of(service.submit(id, req.assertedAt(), auth.getName())));
    }

    @GetMapping("/api/input-plausibility/date-channels/{id}")
    public DateChannelDto get(@PathVariable UUID id) {
        return DateChannelDto.of(service.get(id));
    }

    @GetMapping("/api/input-plausibility/date-channels/{id}/readings")
    public List<DateReadingDto> readings(@PathVariable UUID id) {
        return service.readings(id).stream().map(DateReadingDto::of).toList();
    }

    @GetMapping("/api/input-plausibility/date-channels/{id}/rejected-attempts")
    public List<DateRejectedAttemptDto> rejectedAttempts(@PathVariable UUID id) {
        return service.rejectedAttempts(id).stream().map(DateRejectedAttemptDto::of).toList();
    }

    @ExceptionHandler(PlausibilityException.class)
    public ResponseEntity<ProblemDetail> handle(PlausibilityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
