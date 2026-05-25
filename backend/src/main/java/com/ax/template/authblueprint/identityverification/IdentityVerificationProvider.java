package com.ax.template.authblueprint.identityverification;

/**
 * Provider adapter contract. R54 — IDV-PROVIDER-001.
 *
 * <p>Implementations map a provider-specific JSON payload to the canonical
 * {@link VerifiedIdentityData}. Mapping MUST NOT include the source RRN
 * (IDV-CALLBACK-003).
 *
 * <p>Adapter selection is by {@link #providerName()} — the controller passes
 * the {@code {provider}} path segment to the service, which looks up the
 * matching adapter. Unknown name → {@link IdentityVerificationException}
 * (IDV-PROVIDER-002 → 400 ProblemDetail at controller layer).
 */
public interface IdentityVerificationProvider {

    /** Stable adapter name matching the {@code {provider}} path segment. */
    String providerName();

    /**
     * Decode a provider-specific payload (UTF-8 JSON) into the canonical
     * {@link VerifiedIdentityData} shape. Implementations are pure — no
     * persistence, no logging.
     *
     * @throws IdentityVerificationException on EXTRACTION_FAIL outcome
     */
    VerifiedIdentityData extract(byte[] rawBody);
}
