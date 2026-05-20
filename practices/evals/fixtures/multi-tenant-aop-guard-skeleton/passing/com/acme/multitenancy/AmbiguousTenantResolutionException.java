package com.acme.multitenancy;

/**
 * Thrown by {@link TenantAwareCallbackVerifier} when MORE THAN ONE
 * active tenant's secret produces a matching HMAC for the inbound
 * callback. This is an operational error (signing-key reuse across
 * tenants) — NEVER silently pick one tenant, because either choice
 * is a 50/50 cross-tenant write.
 *
 * <p>Controller response: 500 INTERNAL_SERVER_ERROR with a loud ops
 * alert. The request itself is structurally valid; the catalog of
 * per-tenant secrets is misconfigured. Fork-receiver MUST rotate
 * one of the colliding secrets before the next callback arrives.
 *
 * <p>Anchored at blueprints/multi-tenant-manifest.yaml
 * {@code #callback-tenant-resolution.verifier_contract.failure_modes.multiple_match}.
 */
public class AmbiguousTenantResolutionException extends RuntimeException {

    public AmbiguousTenantResolutionException(String message) {
        super(message);
    }
}
