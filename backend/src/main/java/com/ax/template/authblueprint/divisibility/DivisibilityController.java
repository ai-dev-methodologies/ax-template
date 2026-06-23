package com.ax.template.authblueprint.divisibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * material-divisibility-constraint-l0 thin controller. The acting principal is ALWAYS the
 * authenticated caller (caller-authentication-only-no-userid-param). Delegates to
 * {@link DivisibilityService}.
 */
@RestController
public class DivisibilityController {

    public record DeclareReq(@NotBlank @Size(max = 200) String materialRef,
                             @NotNull DivisibilityPolicyKind kind,
                             int maxScale) {}
    public record CheckReq(@NotNull @Positive BigDecimal quantity) {}

    public record PolicyDto(UUID id, String materialRef, long policyVersion,
                            DivisibilityPolicyKind kind, int maxScale, Instant declaredAt) {
        static PolicyDto of(MaterialDivisibilityPolicy p) {
            return new PolicyDto(p.getId(), p.getMaterialRef(), p.getPolicyVersion(),
                p.getPolicyKind(), p.getMaxScale(), p.getDeclaredAt());
        }
    }
    public record CheckDto(UUID id, String materialRef, BigDecimal submittedQuantity,
                           CheckVerdict verdict, long policyVersion, Instant checkedAt) {
        static CheckDto of(DivisibilityCheck c) {
            return new CheckDto(c.getId(), c.getMaterialRef(), c.getSubmittedQuantity(),
                c.getVerdict(), c.getPolicyVersion(), c.getCheckedAt());
        }
    }

    private final DivisibilityService service;

    public DivisibilityController(DivisibilityService service) {
        this.service = service;
    }

    /** DIV-POLICY-001 — declare (or re-declare → append a version) a material's divisibility policy. */
    @PostMapping("/api/divisibility/policies")
    public ResponseEntity<PolicyDto> declare(@Valid @RequestBody DeclareReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PolicyDto.of(service.declare(req.materialRef(), req.kind(), req.maxScale())));
    }

    /** DIV-INTEGRAL/PRECISION-001 — check a quantity; 422 (rejected, never rounded) if forbidden. */
    @PostMapping("/api/divisibility/materials/{materialRef}/checks")
    public CheckDto check(@PathVariable String materialRef, @Valid @RequestBody CheckReq req) {
        return CheckDto.of(service.check(materialRef, req.quantity()));
    }

    @GetMapping("/api/divisibility/materials/{materialRef}/policy")
    public PolicyDto current(@PathVariable String materialRef) {
        return PolicyDto.of(service.current(materialRef));
    }

    /** DIV-POLICY-001 — the append-only version history, oldest first. */
    @GetMapping("/api/divisibility/materials/{materialRef}/policy-history")
    public List<PolicyDto> history(@PathVariable String materialRef) {
        return service.history(materialRef).stream().map(PolicyDto::of).toList();
    }

    /** DIV-RECORD-001 — every recorded quantity check (verdict + policy version in force). */
    @GetMapping("/api/divisibility/materials/{materialRef}/checks")
    public List<CheckDto> checks(@PathVariable String materialRef) {
        return service.checks(materialRef).stream().map(CheckDto::of).toList();
    }

    @ExceptionHandler(DivisibilityException.class)
    public ResponseEntity<ProblemDetail> handle(DivisibilityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
