/**
 * @ax-template-meta
 * template_id: backend/identity-verification/KcbAdapter
 * layer: backend-domain
 * domain: identity-verification
 * anchors_rule: no-rrn-collection-without-legal-basis.md
 * provenance_class: locked_constraint
 * evidence:
 *   - source_type: external
 *     citation: "KISA 본인인증 가이드라인 — KCB (Korea Credit Bureau) CI/DI callback field mapping specification"
 *     url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
 *   - source_type: external
 *     citation: "개인정보보호법 §24 — 고유식별정보 처리 제한: CI/DI tokens are the only permitted correlation identifiers"
 *     url: "https://www.law.go.kr/법령/개인정보보호법"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   KcbAdapter extracts CI/DI from the KCB (Korea Credit Bureau) 본인인증 callback.
 *   KCB uses connecting_info and duplicate_info as payload field names (provider-specific).
 *   The adapter maps to the canonical VerifiedIdentityData shape (IDV-PROVIDER-001).
 *   RRN must NEVER be extracted or stored — only CI and DI.
 */
package com.example.app.identityverification;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KCB (Korea Credit Bureau) 본인인증 adapter.
 *
 * <p>KCB field name conventions:
 * <ul>
 *   <li>{@code connecting_info} — Connecting Information (64-byte hex)
 *   <li>{@code duplicate_info} — Duplicate Information (64-byte hex)
 *   <li>{@code user_name} — verified legal name
 *   <li>{@code birth_day} — date of birth (YYYYMMDD)
 *   <li>{@code kcb_seq_no} — KCB sequence number (goes to metadata)
 * </ul>
 *
 * <p>Produces identical {@link VerifiedIdentityData} shape as {@link PassAdapter} (IDV-PROVIDER-001).
 */
@Component("kcb")
public class KcbAdapter implements IdentityVerificationProvider {

    @Override
    public String providerName() {
        return "kcb";
    }

    @Override
    @SuppressWarnings("unchecked")
    public VerifiedIdentityData extract(IdentityVerificationDto dto) {
        Map<String, Object> payload = dto.providerPayload() != null
                ? dto.providerPayload()
                : Map.of();

        // KCB-specific field names — map to canonical shape (IDV-PROVIDER-001)
        String ci = nonBlank(dto.ci(), (String) payload.get("connecting_info"),
                "KCB callback missing ci / connecting_info");
        String di = nonBlank(dto.di(), (String) payload.get("duplicate_info"),
                "KCB callback missing di / duplicate_info");
        String name = nonBlank(dto.name(), (String) payload.get("user_name"),
                "KCB callback missing name / user_name");

        LocalDate dob = parseDob(
                dto.dob() != null ? dto.dob() : (String) payload.get("birth_day"));

        // Provider-specific metadata — opaque, non-PII keys only (NOT the RRN)
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "kcb_seq_no",   payload.get("kcb_seq_no"));
        putIfPresent(metadata, "kcb_req_seq",  payload.get("kcb_req_seq"));

        return new VerifiedIdentityData(ci, di, name, dob, Instant.now(), "kcb", metadata);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String nonBlank(String primary, String fallback, String errorMessage) {
        if (primary != null && !primary.isBlank()) return primary;
        if (fallback != null && !fallback.isBlank()) return fallback;
        throw new IdentityVerificationException(errorMessage);
    }

    private LocalDate parseDob(String dob) {
        if (dob == null || dob.isBlank()) return null;
        // KCB format: YYYYMMDD
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
