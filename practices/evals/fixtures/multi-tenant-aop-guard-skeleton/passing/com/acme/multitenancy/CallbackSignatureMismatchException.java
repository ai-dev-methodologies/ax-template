package com.acme.multitenancy;

/**
 * Thrown by {@link TenantAwareCallbackVerifier} when NO active
 * tenant's secret produces a matching HMAC for the inbound
 * callback. The signature_fail audit row carries
 * {@code tenant_id = null} per
 * blueprints/multi-tenant-manifest.yaml
 * {@code #ledger-audit-tenant-scope.policy.append_call_site_contract}.
 *
 * <p>Controller response: per provider contract — typically 200 OK
 * (avoid PG retry storm; the audit row is the durable record) OR
 * 401 (when the PG documents that mismatched signatures should be
 * rejected). The choice is provider-specific, NOT a multi-tenant
 * concern; this exception only signals the verification failure.
 */
public class CallbackSignatureMismatchException extends RuntimeException {

    public CallbackSignatureMismatchException(String message) {
        super(message);
    }
}
