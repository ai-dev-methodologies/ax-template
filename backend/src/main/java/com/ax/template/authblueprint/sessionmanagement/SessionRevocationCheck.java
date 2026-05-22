package com.ax.template.authblueprint.sessionmanagement;

/**
 * SPI consulted by the JWT auth filter (or any other guard) before accepting a token.
 *
 * <p>Trace: SESS-REVOKE-003. Default implementation in {@link DefaultSessionRevocationCheck}
 * looks up the SessionRecord by jti and returns:
 * <ul>
 *   <li>{@code true} when status is REVOKED</li>
 *   <li>{@code true} when expiresAt is in the past (clock-based, status-agnostic)</li>
 *   <li>{@code true} when the jti is UNKNOWN — fail-closed, never fail-open</li>
 *   <li>{@code false} only when status is ACTIVE AND expiresAt is in the future</li>
 * </ul>
 */
public interface SessionRevocationCheck {

    /** @return true iff a JWT carrying this jti MUST be rejected at request time. */
    boolean isRevoked(String jti);
}
