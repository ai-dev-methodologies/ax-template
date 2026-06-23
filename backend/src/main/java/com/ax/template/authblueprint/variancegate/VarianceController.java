package com.ax.template.authblueprint.variancegate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import java.util.UUID;

/**
 * variance-tolerance-band-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link VarianceService}.
 */
@RestController
public class VarianceController {

    public record AppraiseReq(@NotBlank @Size(max = 200) String subject,
                              @NotNull BigDecimal standardValue,
                              @NotNull BigDecimal actualValue,
                              @NotNull @PositiveOrZero BigDecimal lowerTolerance,
                              @NotNull @PositiveOrZero BigDecimal upperTolerance) {}
    // reason is intentionally NOT @NotBlank: a blank reason is a DOMAIN rule (silent acceptance is
    // not permitted, VG-DISPOSE-001 → 422 VARIANCE_BLANK_REASON in the service), not a syntactic 400.
    public record DisposeReq(@Size(max = 1000) String reason) {}

    public record AppraisalDto(UUID id, String subject, BigDecimal standardValue, BigDecimal actualValue,
                               BigDecimal variance, BigDecimal lowerTolerance, BigDecimal upperTolerance,
                               VarianceVerdict verdict, boolean disposed, Instant createdAt) {
        static AppraisalDto of(VarianceAppraisal a) {
            return new AppraisalDto(a.getId(), a.getSubject(), a.getStandardValue(), a.getActualValue(),
                a.getVariance(), a.getLowerTolerance(), a.getUpperTolerance(), a.getVerdict(),
                a.isDisposed(), a.getCreatedAt());
        }
    }
    public record DispositionDto(UUID id, UUID appraisalId, DispositionDecision decision, String actor,
                                 String reason, Instant decidedAt) {
        static DispositionDto of(VarianceDisposition d) {
            return new DispositionDto(d.getId(), d.getAppraisalId(), d.getDecision(), d.getActor(),
                d.getReason(), d.getDecidedAt());
        }
    }

    private final VarianceService service;

    public VarianceController(VarianceService service) {
        this.service = service;
    }

    /** VG-DERIVE-001 + VG-GATE-001 — appraise an actual against a standard through the asymmetric band. */
    @PostMapping("/api/variance-gate/appraisals")
    public ResponseEntity<AppraisalDto> appraise(@Valid @RequestBody AppraiseReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AppraisalDto.of(
            service.appraise(req.subject(), req.standardValue(), req.actualValue(),
                req.lowerTolerance(), req.upperTolerance())));
    }

    /** VG-BLOCK-001 — the dependent operation; blocked 422 while the appraisal is an undisposed breach. */
    @PostMapping("/api/variance-gate/appraisals/{id}/proceed")
    public AppraisalDto proceed(@PathVariable UUID id) {
        return AppraisalDto.of(service.proceed(id));
    }

    /** VG-DISPOSE-001 — record an accountable disposition (actor = caller, reason from the body). */
    @PostMapping("/api/variance-gate/appraisals/{id}/dispositions")
    public AppraisalDto dispose(@PathVariable UUID id, @Valid @RequestBody DisposeReq req,
                                Authentication auth) {
        return AppraisalDto.of(service.dispose(id, auth.getName(), req.reason()));
    }

    @GetMapping("/api/variance-gate/appraisals/{id}")
    public AppraisalDto get(@PathVariable UUID id) {
        return AppraisalDto.of(service.get(id));
    }

    @GetMapping("/api/variance-gate/appraisals/{id}/disposition")
    public ResponseEntity<DispositionDto> disposition(@PathVariable UUID id) {
        VarianceDisposition d = service.dispositionOf(id);
        return d == null ? ResponseEntity.noContent().build()
            : ResponseEntity.ok(DispositionDto.of(d));
    }

    @ExceptionHandler(VarianceException.class)
    public ResponseEntity<ProblemDetail> handle(VarianceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        // VG-BLOCK-001 — a blocked dependent op sees WHY and by HOW MUCH: the variance + named band.
        if (ex.variance() != null) {
            pd.setProperty("variance", ex.variance());
            pd.setProperty("lowerTolerance", ex.lowerTolerance());
            pd.setProperty("upperTolerance", ex.upperTolerance());
        }
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
