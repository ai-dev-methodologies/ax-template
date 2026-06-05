package com.ax.template.authblueprint.secretsmanagement;

import java.time.Instant;
import java.util.Set;

/**
 * One managed secret. Holds the SECRET-ROTATION-001 overlap pair (EXACTLY 2 versions valid during
 * overlap), the SECRET-LIFECYCLE-001 state (ENABLED/DISABLED/DESTROYED), the SECRET-ACCESS-001
 * per-secret ACL (the set of principals granted this specific secret — never a wildcard), and the
 * issued-credential TTL ({@code expiresAt}, NIST cryptoperiod). Every encrypted version is an
 * {@link EnvelopeEncryptedSecret} — the record never holds plaintext.
 *
 * <p>Immutable: every state transition returns a NEW record (copy-on-write). Spec:
 * specs/secrets-management-l0.yaml.
 */
public record SecretRecord(
        String secretId,
        EnvelopeEncryptedSecret current,
        long currentVersion,
        EnvelopeEncryptedSecret previous,   // null when never rotated
        long previousVersion,               // -1 when no previous
        Lifecycle lifecycle,
        Set<String> grantedPrincipals,      // SECRET-ACCESS-001 least-privilege ACL
        boolean revoked,                    // SECRET-LIFECYCLE-001 immediate revocation
        Instant expiresAt) {                // SECRET-LIFECYCLE-001 TTL / cryptoperiod

    public enum Lifecycle { ENABLED, DISABLED, DESTROYED }

    public static SecretRecord created(String secretId, EnvelopeEncryptedSecret sealed,
                                       Set<String> grants, Instant expiresAt) {
        return new SecretRecord(secretId, sealed, 1L, null, -1L,
                Lifecycle.ENABLED, Set.copyOf(grants), false, expiresAt);
    }

    /** SECRET-ROTATION-001 — promote new→current, current→previous, drop the third (atomic via copy). */
    public SecretRecord rotated(EnvelopeEncryptedSecret next, Instant newExpiresAt) {
        return new SecretRecord(secretId, next, currentVersion + 1, current, currentVersion,
                lifecycle, grantedPrincipals, revoked, newExpiresAt);
    }

    public SecretRecord withLifecycle(Lifecycle next) {
        return new SecretRecord(secretId, current, currentVersion, previous, previousVersion,
                next, grantedPrincipals, revoked, expiresAt);
    }

    /** SECRET-LIFECYCLE-001 — immediate revocation; effect is on the next resolve, not next rotation. */
    public SecretRecord revokedNow() {
        return new SecretRecord(secretId, current, currentVersion, previous, previousVersion,
                lifecycle, grantedPrincipals, true, expiresAt);
    }

    public boolean isPrincipalGranted(String principal) {
        return grantedPrincipals.contains(principal);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public boolean isDestroyed() {
        return lifecycle == Lifecycle.DESTROYED;
    }
}
