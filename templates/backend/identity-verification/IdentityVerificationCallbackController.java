/**
 * @ax-template-meta
 * template_id: backend/identity-verification/IdentityVerificationCallbackController
 * layer: backend-domain
 * domain: identity-verification
 * anchors_rule: webhook-hmac-required.md
 * provenance_class: locked_constraint
 * evidence:
 *   - source_type: external
 *     citation: "KISA 본인인증 가이드라인 — callback endpoint must verify provider HMAC before processing CI/DI payload"
 *     url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
 *   - source_type: external
 *     citation: "OWASP ASVS V13.2.6 — Verify that webhook payloads are verified with an HMAC signature before processing"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 *   - source_type: external
 *     citation: "GitHub Docs — Validating webhook deliveries: use @RequestBody byte[] to preserve HMAC integrity"
 *     url: "https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   IdentityVerificationCallbackController handles server-to-server POST from PASS/KCB.
 *   Step 1: verifyHmac (IDV-CALLBACK-001) — rejects with 401 on invalid/absent HMAC.
 *   Step 2: deserialize raw body to IdentityVerificationDto.
 *   Step 3: processCallback → extract CI/DI → persist → audit.
 *   Uses @RequestBody byte[] to preserve HMAC integrity (NOT String or parsed DTO).
 */
package com.example.app.identityverification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Handles server-to-server identity verification callbacks from PASS and KCB providers.
 *
 * <p>Security contract (IDV-CALLBACK-001):
 * <ol>
 *   <li>HMAC-SHA256 verification via X-Identity-Signature header is performed first.
 *   <li>Raw {@code byte[]} body is used — never {@code String} or a pre-parsed DTO — to
 *       preserve the exact byte sequence the provider signed.
 *   <li>Constant-time comparison prevents timing attacks.
 * </ol>
 *
 * <p>Admin endpoint (IDV-ADMIN-001): requires {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api")
public class IdentityVerificationCallbackController {

    private final IdentityVerificationService service;
    private final ObjectMapper objectMapper;

    public IdentityVerificationCallbackController(
            IdentityVerificationService service,
            ObjectMapper objectMapper) {
        this.service      = service;
        this.objectMapper = objectMapper;
    }

    /**
     * Receive a 본인인증 CI/DI callback from a KISA-compliant provider (IDV-CALLBACK-001/002).
     *
     * <p>Uses {@code byte[]} body so the HMAC is computed over the exact bytes the provider signed.
     * Only after successful HMAC verification is the body deserialized.
     *
     * @param provider        URL path segment: "pass" | "kcb"
     * @param signatureHeader X-Identity-Signature: "sha256=<hexdigest>"
     * @param rawBody         raw request bytes (HMAC-preserving)
     * @return 200 on success; 400/401/500 on failure (RFC 7807 ProblemDetail)
     */
    @PostMapping("/identity-verification/callback/{provider}")
    public ResponseEntity<Map<String, Object>> receiveCallback(
            @PathVariable String provider,
            @RequestHeader(value = "X-Identity-Signature", required = false) String signatureHeader,
            @RequestBody byte[] rawBody) throws IOException {

        // Step 1 — HMAC verification (IDV-CALLBACK-001)
        service.verifyHmac(signatureHeader, rawBody, provider);

        // Step 2 — Deserialize after HMAC is confirmed (never before)
        IdentityVerificationDto dto = objectMapper.readValue(rawBody, IdentityVerificationDto.class);

        // Step 3 — Extract CI/DI, persist, audit (IDV-CALLBACK-002, IDV-AUDIT-001)
        VerifiedIdentity saved = service.processCallback(provider, dto);

        return ResponseEntity.ok(Map.of(
                "verifiedAt",   saved.getVerifiedAt().toString(),
                "providerName", saved.getProviderName()
        ));
    }

    /**
     * List verified identity records for admin inspection (IDV-ADMIN-001).
     *
     * <p>Requires {@code ROLE_ADMIN}. Returns paginated {@link VerifiedIdentity} summary.
     */
    @GetMapping("/admin/identity-verification")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<VerifiedIdentityAdminDto>> listVerifiedIdentities(
            @RequestParam(defaultValue = "ALL") String provider,
            Pageable pageable) {
        return ResponseEntity.ok(
                service.list(provider, pageable).map(VerifiedIdentityAdminDto::from)
        );
    }

    /** Admin-safe DTO — exposes CI/DI but never reconstructs the RRN. */
    record VerifiedIdentityAdminDto(
            String id,
            String ci,
            String di,
            String name,
            String dob,
            String providerName,
            Instant verifiedAt) {
        static VerifiedIdentityAdminDto from(VerifiedIdentity v) {
            return new VerifiedIdentityAdminDto(
                    v.getId().toString(),
                    v.getCi(),
                    v.getDi(),
                    v.getName(),
                    v.getDob() != null ? v.getDob().toString() : null,
                    v.getProviderName(),
                    v.getVerifiedAt()
            );
        }
    }
}
