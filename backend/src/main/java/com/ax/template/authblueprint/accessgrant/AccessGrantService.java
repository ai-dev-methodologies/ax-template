package com.ax.template.authblueprint.accessgrant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * time-bounded-access-grant-l0 sole orchestrator. Composes a TIME-BOUNDED RELATIONSHIP GRANT
 * (ReBAC) with a MULTI-CREDENTIAL ELIGIBILITY GATE, both fail-closed against the injected Clock.
 *
 * <p>The defining discipline: 'expired' is NEVER persisted. {@link #check(UUID)} recomputes the
 * verdict from (status, validFrom, validUntil) against {@code Instant.now(clock)} every call via
 * {@link AccessGrant#isActiveAt(Instant)} — so the SAME grant row is allowed at T and denied at
 * validUntil with NO intervening write (AGRANT-WINDOW/BOUNDARY-001). Likewise the eligibility
 * gate recomputes each credential's validity over the Clock (AGRANT-ELIGIBILITY-001). Grants are
 * append-only and revocable under the grant row's PESSIMISTIC_WRITE lock (AGRANT-REVOKE-001);
 * there is NO delete path. Each @Transactional write mutates exactly ONE aggregate.
 */
@Service
public class AccessGrantService {

    private final AccessGrantRepository grants;
    private final CredentialRepository credentials;
    private final AccessGrantMetrics metrics;
    private final Clock clock;

    public AccessGrantService(AccessGrantRepository grants, CredentialRepository credentials,
                              AccessGrantMetrics metrics, Clock clock) {
        this.grants = grants;
        this.credentials = credentials;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public AccessGrant grant(String subjectId, String resourceRef, String relation,
                             Instant validFrom, Instant validUntil) {
        if (!validUntil.isAfter(validFrom)) {
            metrics.record("grant", "invalid");
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
        AccessGrant g = new AccessGrant(UUID.randomUUID(), subjectId, resourceRef, relation,
            validFrom, validUntil, Instant.now(clock));
        AccessGrant saved = grants.save(g);
        metrics.record("grant", "ok");
        return saved;
    }

    /**
     * AGRANT-WINDOW/BOUNDARY-001 — the RECOMPUTED access check. Reads the grant and decides on the
     * injected Clock's now: a revoked grant fails closed (GRANT_REVOKED) regardless of the window;
     * a check before validFrom is GRANT_NOT_YET_VALID; a check at or after validUntil is
     * GRANT_EXPIRED; otherwise (now ∈ [validFrom, validUntil) AND ACTIVE) it is allowed. No write
     * happens here — the verdict is a pure function of the row + the Clock.
     */
    @Transactional(readOnly = true)
    public AccessGrant check(UUID grantId) {
        AccessGrant g = grants.findById(grantId).orElseThrow(AccessGrantException::notFound);
        Instant now = Instant.now(clock);
        if (g.isRevoked()) {
            metrics.record("check", "revoked");
            throw AccessGrantException.revoked();
        }
        if (g.isBeforeWindow(now)) {
            metrics.record("check", "not_yet_valid");
            throw AccessGrantException.notYetValid();
        }
        if (!g.isActiveAt(now)) {                 // ACTIVE + not-before-window but not active ⇒ at/after validUntil
            metrics.record("check", "expired");
            throw AccessGrantException.expired();
        }
        metrics.record("check", "allowed");
        return g;
    }

    /**
     * AGRANT-REVOKE-001 — revoke under the grant row's PESSIMISTIC_WRITE lock so a concurrent
     * revoke records exactly one (actor, instant). Idempotent: a second revoke leaves the first
     * (revokedBy, revokedAt) in place.
     */
    @Transactional
    public AccessGrant revoke(UUID grantId, String actor) {
        AccessGrant g = grants.findByIdForUpdate(grantId).orElseThrow(AccessGrantException::notFound);
        g.revoke(actor, Instant.now(clock));
        metrics.record("revoke", "ok");
        return g;
    }

    @Transactional(readOnly = true)
    public AccessGrant get(UUID grantId) {
        return grants.findById(grantId).orElseThrow(AccessGrantException::notFound);
    }

    @Transactional
    public Credential issueCredential(String subjectId, String credentialClass,
                                      Instant validFrom, Instant validUntil) {
        if (!validUntil.isAfter(validFrom)) {
            metrics.record("credential", "invalid");
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
        Credential c = new Credential(UUID.randomUUID(), subjectId, credentialClass,
            validFrom, validUntil, Instant.now(clock));
        Credential saved = credentials.save(c);
        metrics.record("credential", "ok");
        return saved;
    }

    /**
     * AGRANT-ELIGIBILITY-001 — pass ONLY when, for EVERY required class, the subject holds a
     * credential of that class that is non-expired at the Clock's now. The verdict is recomputed
     * over the Clock (each credential's validity is {@link Credential#isValidAt(Instant)}). A
     * single missing or expired required class fails closed (403 CREDENTIAL_INELIGIBLE) NAMING
     * the first such class. Required classes are checked in their given order for a deterministic
     * named failure.
     */
    @Transactional(readOnly = true)
    public void requireEligible(String subjectId, List<String> requiredClasses) {
        Instant now = Instant.now(clock);
        Set<String> validClasses = new LinkedHashSet<>();
        for (Credential c : credentials.findBySubjectId(subjectId)) {
            if (c.isValidAt(now)) {
                validClasses.add(c.getCredentialClass());
            }
        }
        for (String required : requiredClasses) {
            if (!validClasses.contains(required)) {
                metrics.record("eligibility", "ineligible");
                throw AccessGrantException.credentialIneligible(required);
            }
        }
        metrics.record("eligibility", "eligible");
    }
}
