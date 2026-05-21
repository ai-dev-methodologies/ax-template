package com.ax.template.authblueprint.apikey;

/**
 * Lifecycle states for an {@link ApiKey}.
 *
 * <p>Transitions (manifest {@code lifecycle.transitions}):
 * <pre>
 *   ACTIVE  → REVOKED
 *   REVOKED → (terminal)
 * </pre>
 *
 * <p>Expiration is a clock-based predicate, NOT a state — checked on every
 * authentication attempt (KEY-LIFECYCLE-003).
 */
public enum ApiKeyStatus {
    ACTIVE,
    REVOKED
}
