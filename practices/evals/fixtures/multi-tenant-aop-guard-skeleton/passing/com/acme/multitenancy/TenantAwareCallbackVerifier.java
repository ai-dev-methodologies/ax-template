package com.acme.multitenancy;

import java.util.UUID;

/**
 * Canonical callback verifier interface fixture for
 * blueprints/multi-tenant-manifest.yaml anchor
 * {@code #callback-tenant-resolution.verifier_contract}.
 *
 * <p>Models the GAP-R3-4 closure surface: external PG callbacks
 * (NICE / Toss / KakaoPay webhooks) arrive at {@code permitAll}
 * endpoints with NO authenticated principal — therefore
 * {@code TenantContext.current()} returns
 * {@link java.util.Optional#empty()} at the controller entry.
 * Resolving the originating tenant from forgeable signals (orderId
 * prefix, path segment, X-Tenant-Id header) silently writes a
 * Payment row under the WRONG tenant.
 *
 * <p>The verifier's contract is atomic: signature verification and
 * tenant resolution happen in a single call. Splitting the two
 * operations across separate methods would let a bug accept a
 * callback signed by tenant B's secret but set
 * {@code TenantContext} from tenant A's orderId.
 *
 * <p>The implementation walks the active per-tenant secrets and
 * accepts the request only when EXACTLY ONE secret produces a
 * matching HMAC. The matching secret's owning tenant is returned;
 * the controller MUST call {@code TenantContext.set(...)} on the
 * result before invoking any tenant-scoped service.
 *
 * <p>Audit ordering: the audit-append for signature_fail runs
 * BEFORE this resolution succeeds (see manifest anchor
 * {@code #ledger-audit-tenant-scope}). Orphan audit rows therefore
 * carry {@code tenant_id = null} — the {@link AuditEvent} fixture
 * models this case.
 */
public interface TenantAwareCallbackVerifier {

    /**
     * Verifies a callback signature against the active per-tenant
     * secrets and returns the resolved tenant_id.
     *
     * @param rawBody raw request body bytes (verification MUST run
     *                against the byte payload, not the parsed JSON,
     *                to avoid normalization attacks).
     * @param providerSignatureHeader value of the PG-specific
     *                signature header (NICE: {@code X-Signature};
     *                Toss V1: {@code TossPayments-Signature};
     *                KakaoPay: {@code X-KakaoPay-Signature}).
     * @param providerName provider key matching
     *                {@code PaymentProvider#getType()}.
     * @return the tenant_id whose secret produced the matching HMAC.
     * @throws CallbackSignatureMismatchException when NO active
     *         tenant's secret produces a matching HMAC. The
     *         signature_fail audit row carries
     *         {@code tenant_id = null}.
     * @throws AmbiguousTenantResolutionException when MORE THAN
     *         ONE tenant's secret matches — operational error
     *         (key reuse), never silently pick one.
     */
    UUID verifyAndResolveTenant(
            byte[] rawBody,
            String providerSignatureHeader,
            String providerName);
}
