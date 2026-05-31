package com.ax.template.authblueprint.realtime;

/**
 * RT-CHANNEL-AUTH-002 — a subscribe whose {@code tenantScope} path segment does not
 * match the caller's resolved scope. Mapped to {@code 403 Forbidden} by
 * {@link RealtimeProblemAdvice}. The catalog refuses "global channel" patterns that
 * would leak cross-tenant data the moment a recipe declares {@code tenant_model: multi}.
 */
public class CrossTenantSubscriptionException extends RuntimeException {

    public static final String CODE = "REALTIME_CROSS_TENANT_SUBSCRIPTION";

    public CrossTenantSubscriptionException(String requestedScope) {
        super("subscription scope does not match caller scope: requested=" + requestedScope);
    }
}
