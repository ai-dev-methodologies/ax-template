package com.ax.template.authblueprint.webhooksigning;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * WHSIGN-SECRET-001 — per-endpoint signing secrets with a rotation overlap of EXACTLY 2 active
 * versions. Each endpoint owns its own ≥256-bit secret (never a shared global secret); rotation keeps
 * {@code {current, previous}} so the verifier accepts a signature matching EITHER during the overlap,
 * then the third (oldest) is dropped on the next rotation.
 *
 * <p>Secrets are held as raw {@code byte[]} and are NEVER exposed through any accessor — only the
 * ordered active-candidate list (current-first) for the verifier to loop over with a constant-time
 * compare. In-memory reference (a fork-receiver swaps a KMS / secrets-management-l0 store behind the
 * same contract). Spec: specs/webhook-signing-l0.yaml#WHSIGN-SECRET-001.
 */
@Component
public class SigningSecretStore {

    private static final int SECRET_BYTES = 32; // 256-bit minimum
    private final SecureRandom random = new SecureRandom();

    /** Per-endpoint ordered active secrets: index 0 = current, index 1 (if present) = previous. */
    private final ConcurrentHashMap<String, List<byte[]>> byEndpoint = new ConcurrentHashMap<>();

    /** Provision (or reset) an endpoint with a freshly generated 256-bit secret; returns its hex form ONCE. */
    public String provision(String endpoint) {
        byte[] secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);
        byEndpoint.put(endpoint, List.of(secret.clone()));
        return HexFormat.of().formatHex(secret);
    }

    /**
     * Rotate the endpoint: new→current, old current→previous, drop the third. Returns the NEW secret's
     * hex form ONCE.
     */
    public String rotate(String endpoint) {
        List<byte[]> existing = byEndpoint.get(endpoint);
        if (existing == null) {
            return provision(endpoint);
        }
        byte[] next = new byte[SECRET_BYTES];
        random.nextBytes(next);
        byte[] current = existing.get(0);
        byEndpoint.put(endpoint, List.of(next.clone(), current)); // exactly {current=next, previous=old current}
        return HexFormat.of().formatHex(next);
    }

    /** The ordered (current-first) active candidates for {@code endpoint}, or empty when unknown. */
    List<byte[]> activeSecrets(String endpoint) {
        List<byte[]> secrets = byEndpoint.get(endpoint);
        return secrets == null ? List.of() : secrets;
    }
}
