/**
 * @ax-template-meta
 * template_id: backend/identity-verification/IdentityVerificationProvider
 * layer: backend-domain
 * domain: identity-verification
 * anchors_rule: no-rrn-collection-without-legal-basis.md
 * provenance_class: locked_constraint
 * evidence:
 *   - source_type: external
 *     citation: "KISA 본인인증 가이드라인 — CI (Connecting Information) is a 64-byte hex token that uniquely identifies a person across services without exposing the RRN"
 *     url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
 *   - source_type: external
 *     citation: "개인정보보호법 §24 — 고유식별정보(주민등록번호 등) 처리 제한: collecting or processing RRN requires explicit legal basis"
 *     url: "https://www.law.go.kr/법령/개인정보보호법"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   IdentityVerificationProvider is the vendor-agnostic interface for KISA-compliant
 *   본인인증 adapters (PASS, KCB, future providers).
 *   Each adapter extracts CI/DI from a provider-specific payload and maps it to
 *   the canonical VerifiedIdentityData record.
 *   CRITICAL: neither the interface nor any implementation may accept, return,
 *   or log the raw RRN (주민등록번호). CI and DI are the only permitted identity tokens.
 */
package com.example.app.identityverification;

/**
 * Vendor-agnostic interface for KISA 본인인증 adapters.
 *
 * <p>Implementations: {@link PassAdapter} (PASS 본인인증 — SKT/KT/LGU+),
 * {@link KcbAdapter} (KCB — Korea Credit Bureau).
 *
 * <p>Contract: both adapters MUST produce an identical {@link VerifiedIdentityData} shape.
 * Provider-specific extras go into {@code metadata} map — NEVER into domain fields.
 *
 * <p>RRN prohibition (개인정보보호법 §24 — IDV-CALLBACK-003):
 * The raw Resident Registration Number MUST NOT appear in method signatures,
 * return values, or log statements at any level.
 */
public interface IdentityVerificationProvider {

    /**
     * Extract canonical identity data from a provider-specific callback payload.
     *
     * @param dto the raw callback payload (already HMAC-verified by the controller)
     * @return canonical verified identity data; never null
     * @throws IdentityVerificationException if extraction fails or required fields are absent
     */
    VerifiedIdentityData extract(IdentityVerificationDto dto);

    /**
     * Provider name key matching the URL path segment ({@code /callback/{provider}}).
     *
     * @return "pass" | "kcb" (lowercase, as registered in Spring application context)
     */
    String providerName();
}
