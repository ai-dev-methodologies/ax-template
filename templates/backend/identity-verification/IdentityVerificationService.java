/**
 * @ax-template-meta
 * template_id: backend/identity-verification/IdentityVerificationService
 * layer: backend-domain
 * domain: identity-verification
 * anchors_rule: no-rrn-collection-without-legal-basis.md
 * provenance_class: locked_constraint
 * evidence:
 *   - source_type: external
 *     citation: "KISA 본인인증 가이드라인 — CI/DI are the only permitted identity tokens; raw RRN must never be processed, stored, or logged"
 *     url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
 *   - source_type: external
 *     citation: "개인정보보호법 §24 — 고유식별정보 처리 제한: minimum necessary principle"
 *     url: "https://www.law.go.kr/법령/개인정보보호법"
 *   - source_type: external
 *     citation: "OWASP ASVS V6 — Verification of Stored Cryptography: constant-time comparison for HMAC"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 *   - source_type: external
 *     citation: "GitHub Docs — Validating webhook deliveries: use MessageDigest.isEqual() for constant-time HMAC comparison"
 *     url: "https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   IdentityVerificationService owns all business logic for the identity-verification domain:
 *     - verifyHmac(): constant-time HMAC-SHA256 verification before any processing
 *     - processCallback(): resolve adapter → extract CI/DI → persist VerifiedIdentity → audit
 *   See blueprints/identity-verification-manifest.yaml for policy details.
 */
package com.example.app.identityverification;

import com.example.app.auditlog.AuditLogPublisher;
import com.example.app.common.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/**
 * Business logic for the identity verification (본인인증) domain.
 *
 * <p>Pattern: server-to-server HMAC-verified callback from PASS / KCB providers.
 * CI/DI tokens are extracted and persisted; the raw RRN is never processed (IDV-CALLBACK-003).
 *
 * <p>Extends {@link BaseService} (SP13) for shared exception helpers.
 */
@Service
@Transactional(readOnly = true)
public class IdentityVerificationService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(IdentityVerificationService.class);
    private static final String HMAC_ALGORITHM   = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    /** Provider name → adapter map, injected by Spring (qualifier: provider name). */
    private final Map<String, IdentityVerificationProvider> providerAdapters;
    private final VerifiedIdentityRepository repository;
    private final AuditLogPublisher auditLogPublisher;

    /**
     * Per-provider HMAC secrets.
     * Configure in ax.identity-verification.{provider}.secret (Vault / env).
     * These values must NEVER appear in source code.
     */
    @Value("${ax.identity-verification.pass.secret:REPLACE_WITH_VAULT_SECRET}")
    private String passSecret;

    @Value("${ax.identity-verification.kcb.secret:REPLACE_WITH_VAULT_SECRET}")
    private String kcbSecret;

    public IdentityVerificationService(
            Map<String, IdentityVerificationProvider> providerAdapters,
            VerifiedIdentityRepository repository,
            AuditLogPublisher auditLogPublisher) {
        this.providerAdapters = providerAdapters;
        this.repository       = repository;
        this.auditLogPublisher = auditLogPublisher;
    }

    /**
     * Verify the provider HMAC-SHA256 signature (IDV-CALLBACK-001).
     *
     * <p>Uses constant-time {@link MessageDigest#isEqual} to prevent timing attacks.
     * Throws {@link ResponseStatusException}(401) on mismatch or missing header.
     *
     * @param signatureHeader value of X-Identity-Signature header ("sha256=<hexdigest>")
     * @param rawBody         raw request bytes (must NOT be re-serialised — HMAC is over the original bytes)
     * @param providerName    provider key for secret lookup
     */
    public void verifyHmac(String signatureHeader, byte[] rawBody, String providerName) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or malformed X-Identity-Signature header");
        }
        String expectedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
        byte[] expectedBytes;
        try {
            expectedBytes = HexFormat.of().parseHex(expectedHex);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid HMAC hex encoding");
        }

        byte[] computed = computeHmac(rawBody, secretFor(providerName));
        if (!MessageDigest.isEqual(computed, expectedBytes)) {
            // Log the outcome but NEVER log the signature or secret
            log.warn("Identity verification HMAC mismatch for provider={}", providerName);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HMAC signature invalid");
        }
    }

    /**
     * Process a verified callback: extract CI/DI → persist → audit (IDV-CALLBACK-002).
     *
     * <p>Assumes {@link #verifyHmac} was called before this method.
     *
     * @param providerName the URL path provider segment ("pass" | "kcb")
     * @param dto          deserialized callback payload
     * @return the persisted {@link VerifiedIdentity}
     */
    @Transactional
    public VerifiedIdentity processCallback(String providerName, IdentityVerificationDto dto) {
        IdentityVerificationProvider adapter = resolveAdapter(providerName);
        String outcome = "SUCCESS";
        String ci = null;
        try {
            VerifiedIdentityData data = adapter.extract(dto);
            ci = data.ci();
            VerifiedIdentity entity = VerifiedIdentity.create(data);
            VerifiedIdentity saved = repository.save(entity);
            // Audit (IDV-AUDIT-001) — CI logged; RRN never logged
            auditLogPublisher.publish(auditEvent(providerName, outcome, ci, Instant.now()));
            return saved;
        } catch (IdentityVerificationException e) {
            outcome = "EXTRACTION_FAIL";
            auditLogPublisher.publish(auditEvent(providerName, outcome, null, null));
            throw e;
        } catch (Exception e) {
            outcome = "EXTRACTION_FAIL";
            auditLogPublisher.publish(auditEvent(providerName, outcome, null, null));
            throw new IdentityVerificationException("Extraction failed: " + e.getMessage(), 500);
        }
    }

    /**
     * List verified identities for admin (IDV-ADMIN-001).
     * RBAC enforced in the controller via @PreAuthorize("hasAuthority('ROLE_ADMIN')").
     */
    public Page<VerifiedIdentity> list(String providerFilter, Pageable pageable) {
        if (providerFilter == null || providerFilter.equalsIgnoreCase("ALL")) {
            return repository.findAll(pageable);
        }
        return repository.findByProviderName(providerFilter.toLowerCase(), pageable);
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private IdentityVerificationProvider resolveAdapter(String providerName) {
        IdentityVerificationProvider adapter = providerAdapters.get(providerName);
        if (adapter == null) {
            throw new IdentityVerificationException("Unknown provider: " + providerName, 400);
        }
        return adapter;
    }

    private String secretFor(String providerName) {
        return switch (providerName) {
            case "pass" -> passSecret;
            case "kcb"  -> kcbSecret;
            default     -> throw new IdentityVerificationException(
                    "Unknown provider for secret lookup: " + providerName, 400);
        };
    }

    private byte[] computeHmac(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    private java.util.Map<String, Object> auditEvent(
            String providerName, String outcome, String ci, Instant verifiedAt) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("eventType", "IDENTITY_VERIFICATION_CALLBACK");
        payload.put("providerName", providerName);
        payload.put("outcome", outcome);
        if (ci != null)          payload.put("ci", ci);
        if (verifiedAt != null)  payload.put("verifiedAt", verifiedAt.toString());
        // ⚠ RRN must NEVER be added here — 개인정보보호법 §24 / IDV-AUDIT-001
        return payload;
    }
}
