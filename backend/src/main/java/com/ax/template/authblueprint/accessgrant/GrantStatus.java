package com.ax.template.authblueprint.accessgrant;

/**
 * time-bounded-access-grant-l0 grant status (AGRANT-WINDOW/REVOKE-001). ACTIVE is the only
 * status under which a grant MAY be allowed (and only then while now ∈ [validFrom, validUntil));
 * REVOKED fails closed on every check regardless of the window. Note there is deliberately NO
 * EXPIRED status — 'expired' is a RECOMPUTED predicate over the injected Clock, never a stored
 * state that could go stale.
 */
public enum GrantStatus {
    ACTIVE,
    REVOKED
}
