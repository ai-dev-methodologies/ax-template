/**
 * @ax-template-meta
 * template_id: backend/identity-verification/PassAdapter
 * layer: backend-domain
 * domain: identity-verification
 * anchors_rule: no-rrn-collection-without-legal-basis.md
 * provenance_class: locked_constraint
 * evidence:
 *   - source_type: external
 *     citation: "KISA 본인인증 가이드라인 — PASS (SKT/KT/LGU+ consortium) CI/DI callback field mapping specification"
 *     url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
 *   - source_type: external
 *     citation: "개인정보보호법 §24 — 고유식별정보 처리 제한: CI/DI replace the RRN for identity correlation; raw RRN must not be stored"
 *     url: "https://www.law.go.kr/법령/개인정보보호법"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   PassAdapter extracts CI/DI from the PASS 본인인증 provider callback.
 *   PASS uses ci_value and di_value as payload field names (provider-specific).
 *   The adapter maps to the canonical VerifiedIdentityData shape.
 *   RRN (주민등록번호) must NEVER be extracted or stored — only CI and DI.
 */
package com.example.app.identityverification;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PASS 본인인증 adapter — extracts CI/DI from PASS (SKT/KT/LGU+) callback payloads.
 *
 * <p>PASS field name conventions (per KISA PASS spec):
 * <ul>
 *   <li>{@code ci_value} — Connecting Information (64-byte hex)
 *   <li>{@code di_value} — Duplicate Information (64-byte hex)
 *   <li>{@code user_name} — verified legal name
 *   <li>{@code birth_date} — date of birth (YYYYMMDD)
 *   <li>{@code pass_request_no} — PASS transaction reference (goes to metadata)
 * </ul>
 *
 * <p>The raw RRN is never extracted or stored (IDV-CALLBACK-003).
 */
@Component("pass")
public class PassAdapter implements IdentityVerificationProvider {

    @Override
    public String providerName() {
        return "pass";
    }

    @Override
    @SuppressWarnings("unchecked")
    public VerifiedIdentityData extract(IdentityVerificationDto dto) {
        Map<String, Object> payload = dto.providerPayload() != null
                ? dto.providerPayload()
                : Map.of();

        // PASS-specific field names — map to canonical shape
        String ci = nonBlank(dto.ci(), (String) payload.get("ci_value"),
                "PASS callback missing ci / ci_value");
        String di = nonBlank(dto.di(), (String) payload.get("di_value"),
                "PASS callback missing di / di_value");
        String name = nonBlank(dto.name(), (String) payload.get("user_name"),
                "PASS callback missing name / user_name");

        LocalDate dob = parseDob(
                dto.dob() != null ? dto.dob() : (String) payload.get("birth_date"));

        // Provider-specific metadata — opaque, non-PII keys only (NOT the RRN)
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "pass_request_no", payload.get("pass_request_no"));
        putIfPresent(metadata, "pass_auth_token",  payload.get("pass_auth_token"));

        return new VerifiedIdentityData(ci, di, name, dob, Instant.now(), "pass", metadata);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String nonBlank(String primary, String fallback, String errorMessage) {
        if (primary != null && !primary.isBlank()) return primary;
        if (fallback != null && !fallback.isBlank()) return fallback;
        throw new IdentityVerificationException(errorMessage);
    }

    private LocalDate parseDob(String dob) {
        if (dob == null || dob.isBlank()) return null;
        // PASS format: YYYYMMDD
        if (dob.length() == 8 && !dob.contains("-")) {
            return LocalDate.of(
                    Integer.parseInt(dob.substring(0, 4)),
                    Integer.parseInt(dob.substring(4, 6)),
                    Integer.parseInt(dob.substring(6, 8))
            );
        }
        return LocalDate.parse(dob); // ISO-8601 fallback
    }

    private void putIfPresent(Map<String, String> map, String key, Object value) {
        if (value != null) map.put(key, value.toString());
    }
}
