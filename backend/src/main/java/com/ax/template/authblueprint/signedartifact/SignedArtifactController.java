package com.ax.template.authblueprint.signedartifact;

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
import java.util.Map;
import java.util.UUID;

/**
 * signed-artifact-l0 thin controller. {@code /issue} is an authenticated ISSUER action;
 * {@code /verify} and {@code /jwks} are the THIRD-PARTY-facing surface (a relying party who is
 * NOT the signer verifies with no access to any issuer secret) — SecurityConfig should route
 * these permitAll, mirroring the spec's "verified by a party OTHER than the signer" posture.
 * Delegates to {@link SignedArtifactService}.
 */
@RestController
public class SignedArtifactController {

    public record IssueReq(@NotBlank @Size(max = 200) String subjectRef,
                           @NotBlank @Size(max = 4000) String content) {}

    public record VerifyReq(@NotBlank @Size(max = 8192) String jws, @NotBlank @Size(max = 4000) String content) {}

    public record ArtifactDto(UUID id, String subjectRef, String contentHash, String kid, String alg,
                              String jws, Instant issuedAt) {
        static ArtifactDto of(SignedArtifact a) {
            return new ArtifactDto(a.getId(), a.getSubjectRef(), a.getContentHash(), a.getKid(), a.getAlg(),
                a.getJws(), a.getIssuedAt());
        }
    }

    public record VerifyDto(boolean valid, String kid) {}

    private final SignedArtifactService service;
    private final SignedArtifactKeyProvider keyProvider;

    public SignedArtifactController(SignedArtifactService service, SignedArtifactKeyProvider keyProvider) {
        this.service = service;
        this.keyProvider = keyProvider;
    }

    /** SIGNED-ASYM-001 — issue a detached asymmetric (ES256) signature over the content-hash. */
    @PostMapping("/api/signed-artifact/records")
    public ResponseEntity<ArtifactDto> issue(@Valid @RequestBody IssueReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ArtifactDto.of(service.issue(req.subjectRef(), req.content())));
    }

    @GetMapping("/api/signed-artifact/records/{id}")
    public ArtifactDto get(@PathVariable UUID id) {
        return ArtifactDto.of(service.getOrThrow(id));
    }

    /** SIGNED-ALG-ALLOWLIST-001 — third-party verification; no issuer secret reachable here. */
    @PostMapping("/api/signed-artifact/verify")
    public VerifyDto verify(@Valid @RequestBody VerifyReq req) {
        service.verify(req.jws(), req.content());
        return new VerifyDto(true, keyProvider.keyId());
    }

    /** SIGNED-ASYM-001 — the published verifying-key set (public-only; no issuer secret). */
    @GetMapping("/api/signed-artifact/jwks")
    public Map<String, Object> jwks() {
        return keyProvider.publicJwkSet().toJSONObject();
    }

    @ExceptionHandler(SignedArtifactException.class)
    public ResponseEntity<ProblemDetail> handle(SignedArtifactException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
