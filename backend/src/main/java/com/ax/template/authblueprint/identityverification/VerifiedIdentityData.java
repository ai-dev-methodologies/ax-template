package com.ax.template.authblueprint.identityverification;

import java.time.Instant;
import java.util.Map;

/**
 * Canonical, provider-neutral payload extracted from a provider callback.
 *
 * <p>R54 — IDV-PROVIDER-001 contract: both PassAdapter and KcbAdapter produce
 * this exact shape. Provider-specific extras live only in {@code metadata};
 * they MUST NOT leak into named fields.
 *
 * <p>IDV-CALLBACK-003 hardening: the shape has fields {@code ci} and {@code di}
 * (opaque correlation tokens) but no field that would re-construct the source
 * RRN — no {@code rrn}, no {@code residentRegistrationNumber}, no
 * {@code 주민등록번호}. Both fields are 64-hex strings produced by the provider.
 */
public record VerifiedIdentityData(
        String ci,
        String di,
        String name,
        String dob,
        Instant verifiedAt,
        String providerName,
        Map<String, String> metadata) {

    public VerifiedIdentityData {
        if (ci == null || ci.isBlank()) {
            throw new IllegalArgumentException("ci must be present");
        }
        if (di == null || di.isBlank()) {
            throw new IllegalArgumentException("di must be present");
        }
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("providerName must be present");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
