package com.ax.template.authblueprint.secretsmanagement;

import com.ax.template.authblueprint.auditlog.AuditLog;
import com.ax.template.authblueprint.auditlog.AuditLogService;
import com.ax.template.authblueprint.auditlog.AuditOutcome;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The sole mutator + resolver for managed secrets. Composes:
 * <ul>
 *   <li>{@link EnvelopeCrypto} — every stored version is ciphertext (SECRET-ENCRYPTION-001);</li>
 *   <li>per-secret ACL on {@link SecretRecord} — least-privilege reads (SECRET-ACCESS-001);</li>
 *   <li>{@link AuditLogService} — every read (grant AND denial) is audited, value-free (SECRET-ACCESS-001);</li>
 *   <li>2-version overlap (SECRET-ROTATION-001) + revocation/TTL (SECRET-LIFECYCLE-001);</li>
 *   <li>{@link SecretMetrics} — bounded access/rotation/resolution-failure counters (SECRET-OBSERVABILITY-001).</li>
 * </ul>
 *
 * <p>In-memory {@link ConcurrentHashMap} reference store — a fork-receiver swaps Vault / AWS Secrets
 * Manager behind the same method surface. Spec: specs/secrets-management-l0.yaml.
 */
@Service
public class SecretService {

    /** AUDIT actions composed into audit-log-l0 (SECRET-ACCESS-001). */
    public static final String ACTION_READ = "SECRET_READ";
    public static final String ACTION_DENIED = "SECRET_ACCESS_DENIED";
    public static final String ACTION_ROTATE = "SECRET_ROTATE";
    public static final String ACTION_REVOKE = "SECRET_REVOKE";
    public static final String RESOURCE_TYPE = "secret";

    /** Default issued-credential TTL (NIST cryptoperiod). Recipe: secrets_default_ttl_hours. */
    static final Duration DEFAULT_TTL = Duration.ofHours(720);

    private final EnvelopeCrypto crypto;
    private final AuditLogService auditLog;
    private final SecretMetrics metrics;
    private final ConcurrentHashMap<String, SecretRecord> store = new ConcurrentHashMap<>();

    public SecretService(EnvelopeCrypto crypto, AuditLogService auditLog, SecretMetrics metrics) {
        this.crypto = crypto;
        this.auditLog = auditLog;
        this.metrics = metrics;
    }

    /** Create a secret sealed under a fresh DEK, granted only to {@code grantedPrincipals}. */
    public SecretRecord create(String secretId, SecretValue plaintext, Set<String> grantedPrincipals) {
        return create(secretId, plaintext, grantedPrincipals, DEFAULT_TTL);
    }

    public SecretRecord create(String secretId, SecretValue plaintext,
                               Set<String> grantedPrincipals, Duration ttl) {
        EnvelopeEncryptedSecret sealed = crypto.seal(plaintext);
        SecretRecord rec = SecretRecord.created(secretId, sealed, grantedPrincipals,
                Instant.now().plus(ttl));
        store.put(secretId, rec);
        return rec;
    }

    /**
     * SECRET-ACCESS-001 — least-privilege read. Authorizes {@code principal} for THIS specific secret,
     * audits both grant and denial (value-free), and records the bounded access metric. Returns the
     * decrypted CURRENT value (plaintext lives only in the returned {@link SecretValue}).
     */
    public SecretValue read(String secretId, String principal) {
        SecretRecord rec = store.get(secretId);
        if (rec == null || rec.isDestroyed()) {
            metrics.resolutionFailure("not_found");
            throw new SecretException(SecretException.Kind.NOT_FOUND, "Secret not found.");
        }
        if (!rec.isPrincipalGranted(principal)) {
            audit(ACTION_DENIED, secretId, principal, AuditOutcome.FAILURE);
            metrics.access("denied");
            throw new SecretException(SecretException.Kind.ACCESS_DENIED,
                    "Principal is not authorized for this secret.");
        }
        assertResolvable(rec);                         // LIFECYCLE: revoked / expired checked on resolve
        audit(ACTION_READ, secretId, principal, AuditOutcome.SUCCESS);
        metrics.access("granted");
        return crypto.open(rec.current());
    }

    /**
     * SECRET-ROTATION-001 — rotate to a new version with a 2-version overlap. The previous version
     * stays valid (overlap) until the next rotation drops it. Atomic via copy-on-write replace.
     */
    public SecretRecord rotate(String secretId, SecretValue newPlaintext, String principal) {
        SecretRecord rec = requireGranted(secretId, principal);
        try {
            EnvelopeEncryptedSecret sealed = crypto.seal(newPlaintext);
            SecretRecord rotated = rec.rotated(sealed, Instant.now().plus(DEFAULT_TTL));
            store.put(secretId, rotated);
            audit(ACTION_ROTATE, secretId, principal, AuditOutcome.SUCCESS);
            metrics.rotation("success");
            return rotated;
        } catch (RuntimeException ex) {
            metrics.rotation("failure");
            throw ex;
        }
    }

    /**
     * SECRET-ROTATION-001 verification — a presented candidate is accepted iff it matches the CURRENT
     * or (during overlap) the PREVIOUS version. A version already retired (dropped past overlap) is
     * rejected → {@link SecretException.Kind#VERSION_RETIRED}.
     */
    public void verifyPresented(String secretId, String principal, SecretValue candidate) {
        SecretRecord rec = requireGranted(secretId, principal);
        assertResolvable(rec);
        if (crypto.open(rec.current()).equals(candidate)) {
            return; // current accepted
        }
        if (rec.previous() != null && crypto.open(rec.previous()).equals(candidate)) {
            return; // previous accepted DURING overlap
        }
        metrics.resolutionFailure("expired");
        throw new SecretException(SecretException.Kind.VERSION_RETIRED,
                "The presented secret version has been retired.");
    }

    /** SECRET-LIFECYCLE-001 — immediate revocation; the next resolve fails-closed, not at next rotation. */
    public void revoke(String secretId, String principal) {
        SecretRecord rec = requireGranted(secretId, principal);
        store.put(secretId, rec.revokedNow());
        audit(ACTION_REVOKE, secretId, principal, AuditOutcome.SUCCESS);
    }

    /** SECRET-LIFECYCLE-001 — destroy: a destroyed secret is unrecoverable (subsequent reads → not found). */
    public void destroy(String secretId, String principal) {
        SecretRecord rec = requireGranted(secretId, principal);
        store.put(secretId, rec.withLifecycle(SecretRecord.Lifecycle.DESTROYED));
    }

    /**
     * SECRET-ENCRYPTION-001 — grant-checked accessor to the AT-REST record so a caller can expose the
     * CIPHERTEXT form without revealing plaintext. Returns the record (which holds only ciphertext +
     * the wrapped DEK), not the plaintext.
     */
    public SecretRecord snapshotForOwner(String secretId, String principal) {
        return requireGranted(secretId, principal);
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private SecretRecord requireGranted(String secretId, String principal) {
        SecretRecord rec = store.get(secretId);
        if (rec == null || rec.isDestroyed()) {
            metrics.resolutionFailure("not_found");
            throw new SecretException(SecretException.Kind.NOT_FOUND, "Secret not found.");
        }
        if (!rec.isPrincipalGranted(principal)) {
            audit(ACTION_DENIED, secretId, principal, AuditOutcome.FAILURE);
            metrics.access("denied");
            throw new SecretException(SecretException.Kind.ACCESS_DENIED,
                    "Principal is not authorized for this secret.");
        }
        return rec;
    }

    /** Fail-closed on revoked/expired, checked on EVERY resolve (LIFECYCLE-001). */
    private void assertResolvable(SecretRecord rec) {
        if (rec.revoked()) {
            metrics.resolutionFailure("revoked");
            throw new SecretException(SecretException.Kind.REVOKED, "Secret has been revoked.");
        }
        if (rec.isExpired(Instant.now())) {
            metrics.resolutionFailure("expired");
            throw new SecretException(SecretException.Kind.EXPIRED, "Secret has expired.");
        }
    }

    /**
     * SECRET-ACCESS-001 — value-free audit record via audit-log-l0. The row stores secret_id only;
     * the secret value is NEVER an audit field. REQUIRES_NEW isolation is inherited from
     * {@link AuditLogService#record}.
     */
    private void audit(String action, String secretId, String principal, AuditOutcome outcome) {
        auditLog.record(AuditLog.builder()
                .actorUserId(principal)
                .action(action)
                .resourceType(RESOURCE_TYPE)
                .resourceId(secretId)        // secret_id only — never the value
                .outcome(outcome)
                .build());
    }
}
