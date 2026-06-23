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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * self-reported-input-plausibility-l0 thin controller. The acting principal is ALWAYS the
 * authenticated caller (caller-authentication-only-no-userid-param). Delegates to
 * {@link PlausibilityService}.
 */
@RestController
public class PlausibilityController {

    public record DefineReq(@NotBlank @Size(max = 200) String subjectRef,
                            @NotNull BigDecimal minValue,
                            @NotNull BigDecimal maxValue,
                            @NotNull BigDecimal maxDeltaPerSecond) {}
    public record SubmitReq(@NotNull BigDecimal reportedValue) {}

    public record ChannelDto(UUID id, String subjectRef, BigDecimal minValue, BigDecimal maxValue,
                             BigDecimal maxDeltaPerSecond, BigDecimal priorValue, Instant priorAt, Long version) {
        static ChannelDto of(PlausibilityChannel c) {
            return new ChannelDto(c.getId(), c.getSubjectRef(), c.getMinValue(), c.getMaxValue(),
                c.getMaxDeltaPerSecond(), c.getPriorValue(), c.getPriorAt(), c.getVersion());
        }
    }
    public record ReadingDto(UUID id, BigDecimal reportedValue, VerificationStatus verificationStatus,
                             String checksRan, boolean hadPrior, BigDecimal priorValue,
                             long elapsedSeconds, BigDecimal computedRate, String actor, Instant occurredAt) {
        static ReadingDto of(PlausibilityReading r) {
            return new ReadingDto(r.getId(), r.getReportedValue(), r.getVerificationStatus(),
                r.getChecksRan(), r.isHadPrior(), r.getPriorValue(), r.getElapsedSeconds(),
                r.getComputedRate(), r.getActor(), r.getOccurredAt());
        }
    }
    public record RejectedAttemptDto(UUID id, BigDecimal reportedValue, RejectReason reason,
                                     BigDecimal priorValue, long elapsedSeconds, BigDecimal computedRate,
                                     String actor, Instant occurredAt) {
        static RejectedAttemptDto of(RejectedAttempt a) {
            return new RejectedAttemptDto(a.getId(), a.getReportedValue(), a.getReason(),
                a.getPriorValue(), a.getElapsedSeconds(), a.getComputedRate(), a.getActor(),
                a.getOccurredAt());
        }
    }

    private final PlausibilityService service;

    public PlausibilityController(PlausibilityService service) {
        this.service = service;
    }

    @PostMapping("/api/input-plausibility/channels")
    public ResponseEntity<ChannelDto> define(@Valid @RequestBody DefineReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ChannelDto.of(
            service.define(req.subjectRef(), req.minValue(), req.maxValue(), req.maxDeltaPerSecond())));
    }

    /** PLAUSIBILITY-RANGE/RATE/PROVENANCE/REJECT-001 — plausibility-gate one self-reported submission. */
    @PostMapping("/api/input-plausibility/channels/{id}/submissions")
    public ResponseEntity<ReadingDto> submit(@PathVariable UUID id, @Valid @RequestBody SubmitReq req,
                                             Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ReadingDto.of(service.submit(id, req.reportedValue(), auth.getName())));
    }

    @GetMapping("/api/input-plausibility/channels/{id}")
    public ChannelDto get(@PathVariable UUID id) {
        return ChannelDto.of(service.get(id));
    }

    @GetMapping("/api/input-plausibility/channels/{id}/readings")
    public List<ReadingDto> readings(@PathVariable UUID id) {
        return service.readings(id).stream().map(ReadingDto::of).toList();
    }

    @GetMapping("/api/input-plausibility/channels/{id}/rejected-attempts")
    public List<RejectedAttemptDto> rejectedAttempts(@PathVariable UUID id) {
        return service.rejectedAttempts(id).stream().map(RejectedAttemptDto::of).toList();
    }

    @ExceptionHandler(PlausibilityException.class)
    public ResponseEntity<ProblemDetail> handle(PlausibilityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
