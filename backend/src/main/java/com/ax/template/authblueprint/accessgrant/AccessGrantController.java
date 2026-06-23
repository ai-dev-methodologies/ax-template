package com.ax.template.authblueprint.accessgrant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
 * time-bounded-access-grant-l0 thin controller. The acting principal (e.g. the revoker) is ALWAYS
 * the authenticated caller (caller-authentication-only-no-userid-param). Delegates to
 * {@link AccessGrantService}. A successful eligibility check returns 204; a failure surfaces as a
 * 403 problem+json naming the offending credential class.
 */
@RestController
public class AccessGrantController {

    public record GrantReq(@NotBlank @Size(max = 200) String subjectId,
                           @NotBlank @Size(max = 200) String resourceRef,
                           @NotBlank @Size(max = 100) String relation,
                           @NotNull Instant validFrom,
                           @NotNull Instant validUntil) {}

    public record CredentialReq(@NotBlank @Size(max = 200) String subjectId,
                                @NotBlank @Size(max = 100) String credentialClass,
                                @NotNull Instant validFrom,
                                @NotNull Instant validUntil) {}

    public record EligibilityReq(@NotBlank @Size(max = 200) String subjectId,
                                 @NotEmpty List<@NotBlank @Size(max = 100) String> requiredClasses) {}

    public record GrantDto(UUID id, String subjectId, String resourceRef, String relation,
                           Instant validFrom, Instant validUntil, GrantStatus status,
                           String revokedBy, Instant revokedAt) {
        static GrantDto of(AccessGrant g) {
            return new GrantDto(g.getId(), g.getSubjectId(), g.getResourceRef(), g.getRelation(),
                g.getValidFrom(), g.getValidUntil(), g.getStatus(), g.getRevokedBy(), g.getRevokedAt());
        }
    }

    public record CredentialDto(UUID id, String subjectId, String credentialClass,
                                Instant validFrom, Instant validUntil) {
        static CredentialDto of(Credential c) {
            return new CredentialDto(c.getId(), c.getSubjectId(), c.getCredentialClass(),
                c.getValidFrom(), c.getValidUntil());
        }
    }

    private final AccessGrantService service;

    public AccessGrantController(AccessGrantService service) {
        this.service = service;
    }

    @PostMapping("/api/access-grant/grants")
    public ResponseEntity<GrantDto> grant(@Valid @RequestBody GrantReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GrantDto.of(
            service.grant(req.subjectId(), req.resourceRef(), req.relation(),
                req.validFrom(), req.validUntil())));
    }

    /** AGRANT-WINDOW/BOUNDARY-001 — the recomputed access check; 200 allowed or 403 (closed). */
    @GetMapping("/api/access-grant/grants/{id}/check")
    public GrantDto check(@PathVariable UUID id) {
        return GrantDto.of(service.check(id));
    }

    /** AGRANT-REVOKE-001 — revoke the grant; the revoking actor is the authenticated caller. */
    @PostMapping("/api/access-grant/grants/{id}/revoke")
    public GrantDto revoke(@PathVariable UUID id, Authentication auth) {
        return GrantDto.of(service.revoke(id, auth.getName()));
    }

    @GetMapping("/api/access-grant/grants/{id}")
    public GrantDto get(@PathVariable UUID id) {
        return GrantDto.of(service.get(id));
    }

    @PostMapping("/api/access-grant/credentials")
    public ResponseEntity<CredentialDto> issueCredential(@Valid @RequestBody CredentialReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CredentialDto.of(
            service.issueCredential(req.subjectId(), req.credentialClass(),
                req.validFrom(), req.validUntil())));
    }

    /** AGRANT-ELIGIBILITY-001 — 204 when every required class is held + valid at now; else 403. */
    @PostMapping("/api/access-grant/eligibility")
    public ResponseEntity<Void> eligibility(@Valid @RequestBody EligibilityReq req) {
        service.requireEligible(req.subjectId(), req.requiredClasses());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AccessGrantException.class)
    public ResponseEntity<ProblemDetail> handle(AccessGrantException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
