package com.ax.template.authblueprint.decisiongov;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * decision-governance-l0 thin controller. The acting identity (decidedBy) is ALWAYS the
 * authenticated caller (caller-authentication-only-no-userid-param); the four-eyes
 * {@code approvedBy} is a request field by necessity — it names the SECOND person — and
 * the service rejects approver == caller (DG-OVERRIDE-001). Delegates to {@link DecisionService}.
 */
@RestController
public class DecisionController {

    public record ComputeReq(@NotBlank @Size(max = 200) String scopeKey,
                             @NotBlank @Size(max = 4000) String basisJson,
                             @NotBlank @Size(max = 500) String outcome) {}
    // reason/approver are validated by the service with domain 422 codes (not a generic 400),
    // mirroring the register-domain posture for governed-exception fields.
    public record RecomputeReq(@NotBlank @Size(max = 4000) String basisJson,
                               @NotBlank @Size(max = 500) String outcome,
                               @Size(max = 1000) String reason) {}
    public record OverrideReq(@NotBlank @Size(max = 500) String outcome,
                              @Size(max = 1000) String reason,
                              @Size(max = 200) String approvedBy) {}

    public record VersionDto(UUID id, int versionNo, DecisionKind kind, String basisJson,
                             String outcome, String reason, String decidedBy, String approvedBy,
                             Instant decidedAt) {
        static VersionDto of(DecisionVersion v) {
            return new VersionDto(v.getId(), v.getVersionNo(), v.getKind(), v.getBasisJson(),
                v.getOutcome(), v.getReason(), v.getDecidedBy(), v.getApprovedBy(), v.getDecidedAt());
        }
    }

    private final DecisionService service;

    public DecisionController(DecisionService service) {
        this.service = service;
    }

    @PostMapping("/api/decisions")
    public ResponseEntity<VersionDto> compute(@Valid @RequestBody ComputeReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(VersionDto.of(service.compute(req.scopeKey(), req.basisJson(), req.outcome(),
                auth.getName())));
    }

    @PostMapping("/api/decisions/{scopeKey}/recompute")
    public ResponseEntity<VersionDto> recompute(@PathVariable String scopeKey,
                                                @Valid @RequestBody RecomputeReq req,
                                                Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(VersionDto.of(service.recompute(scopeKey, req.basisJson(), req.outcome(),
                req.reason(), auth.getName())));
    }

    @PostMapping("/api/decisions/{scopeKey}/override")
    public ResponseEntity<VersionDto> override(@PathVariable String scopeKey,
                                               @Valid @RequestBody OverrideReq req,
                                               Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(VersionDto.of(service.override(scopeKey, req.outcome(), req.reason(),
                auth.getName(), req.approvedBy())));
    }

    @GetMapping("/api/decisions/{scopeKey}")
    public VersionDto latest(@PathVariable String scopeKey) {
        return VersionDto.of(service.latest(scopeKey));
    }

    @GetMapping("/api/decisions/{scopeKey}/versions")
    public PageEnvelope<VersionDto> versions(@PathVariable String scopeKey,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.versions(scopeKey, page, size), VersionDto::of);
    }

    @ExceptionHandler(DecisionException.class)
    public ResponseEntity<ProblemDetail> handle(DecisionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
