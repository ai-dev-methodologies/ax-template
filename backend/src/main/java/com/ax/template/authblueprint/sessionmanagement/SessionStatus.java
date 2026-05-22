package com.ax.template.authblueprint.sessionmanagement;

/**
 * Lifecycle states for a {@link SessionRecord}.
 *
 * <p>Expiration is NOT a state — it's a clock-based predicate
 * ({@link SessionRecord#isExpired}). A session can be ACTIVE-but-expired or
 * REVOKED-and-expired; the SPI treats both as revoked at request time.
 */
public enum SessionStatus {
    ACTIVE,
    REVOKED
}
