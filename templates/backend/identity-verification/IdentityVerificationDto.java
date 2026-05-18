/**
 * @ax-template-meta
 * template_id: backend/identity-verification/IdentityVerificationDto
 * layer: backend-domain
 * domain: identity-verification
 * anchors_rule: no-rrn-collection-without-legal-basis.md
 * provenance_class: locked_constraint
 * evidence:
 *   - source_type: external
 *     citation: "KISA 본인인증 가이드라인 — CI/DI callback payload schema"
 *     url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
 *   - source_type: external
 *     citation: "JEP 395 — Records: immutable data carriers with automatically derived members"
 *     url: "https://openjdk.org/jeps/395"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   IdentityVerificationDto carries the raw provider callback payload parsed from JSON.
 *   CRITICAL: providerPayload is a raw Map — never deserialize into an RRN-containing type.
 *   VerifiedIdentityData is the canonical output record from adapter.extract().
 */
package com.example.app.identityverification;

import java.util.Map;

/**
 * Raw identity verification callback payload from a KISA 본인인증 provider.
 *
 * <p>This is the deserialized inbound JSON from PASS / KCB server-to-server callback.
 * Adapters ({@link PassAdapter}, {@link KcbAdapter}) read {@code providerPayload} and
 * produce {@link VerifiedIdentityData}.
 *
 * <p>The raw payload may contain provider-specific field names for CI/DI. Adapters
 * are responsible for normalizing those to the canonical shape.
 */
public record IdentityVerificationDto(
    /**
     * Provider-assigned transaction identifier for this verification event.
     * Used for audit logging and idempotency checks.
     */
    String transactionId,

    /**
     * CI (Connecting Information) if the provider sends it at the top level.
     * Some providers embed CI inside {@code providerPayload}; adapters should check both.
     */
    String ci,

    /**
     * DI (Duplicate Information) if the provider sends it at the top level.
     */
    String di,

    /**
     * Verified legal name (Korean full name) if provided at the top level.
     */
    String name,

    /**
     * Date of birth as a string (YYYY-MM-DD) if provided at the top level.
     */
    String dob,

    /**
     * Raw provider-specific payload forwarded verbatim from the provider server.
     * Adapters read this map to extract CI/DI under provider-specific field names.
     * MUST NOT contain or be mapped to an RRN field.
     */
    Map<String, Object> providerPayload
) {}

/**
 * Canonical verified identity data extracted by an adapter.
 *
 * <p>Both {@link PassAdapter} and {@link KcbAdapter} MUST produce an instance of this
 * record with identical field semantics (IDV-PROVIDER-001).
 *
 * <p>This record is the input to {@link VerifiedIdentity#create(VerifiedIdentityData)}.
 */
record VerifiedIdentityData(
    /** CI (Connecting Information) — 64-byte hex; cross-service unique person token. */
    String ci,

    /** DI (Duplicate Information) — 64-byte hex; per-service unique person token. */
    String di,

    /** Verified legal name. */
    String name,

    /** Date of birth. */
    java.time.LocalDate dob,

    /** Server-side timestamp of extraction. */
    java.time.Instant verifiedAt,

    /** Provider name: "pass" | "kcb". */
    String providerName,

    /**
     * Provider-specific metadata (NOT the RRN).
     * Only opaque, non-PII provider keys (e.g., "pass_request_no", "kcb_seq_no").
     */
    java.util.Map<String, String> metadata
) {}

/**
 * Exception thrown when identity verification processing fails.
 *
 * <p>Carries an HTTP status code for RFC 7807 ProblemDetail mapping.
 */
class IdentityVerificationException extends RuntimeException {
    private final int statusCode;

    IdentityVerificationException(String message) {
        super(message);
        this.statusCode = 400;
    }

    IdentityVerificationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    int getStatusCode() { return statusCode; }
}
