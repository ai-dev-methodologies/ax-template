/**
 * @ax-template-meta
 * template_id: backend/billing/BillingProvider
 * layer: backend-domain
 * domain: billing
 * anchors_rule: currency-amount-precision-explicit.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Plan / Price model"
 *     quote: "A Price defines the recurring amount, currency, and interval."
 *   - source_type: upstream_id
 *     upstream_id: toss-billing-2026-05
 *     section: "빌링키 발급"
 *     quote: "빌링키를 사용해 결제를 실행한다."
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   BillingProvider is the abstraction layer between the billing service and
 *   Stripe / Toss provider implementations.
 *   No provider-specific fields must leak through this interface.
 *   All amounts are integer minor units (KRW: won, USD: cents).
 */
package com.example.app.billing;

import java.util.UUID;

/**
 * BillingProvider — abstraction for recurring billing operations.
 *
 * <p>Implementations: {@link StripeBillingAdapter}, {@link TossBillingAdapter}.
 *
 * <p>Contract:
 * <ul>
 *   <li>All amounts are integer minor units (no float / BigDecimal).</li>
 *   <li>The returned {@link ProviderSubscriptionResult} contains only canonical fields
 *       — no Stripe-specific or Toss-specific fields leak out.</li>
 *   <li>Provider failures must be wrapped in {@link BillingProviderException}.</li>
 * </ul>
 */
public interface BillingProvider {

    /**
     * Returns the provider name (e.g., "stripe", "toss").
     * Used for observability tagging.
     */
    String providerName();

    /**
     * Creates a recurring subscription with the provider.
     *
     * @param subscriptionId  ax-template subscription UUID (for correlation)
     * @param planAmount      plan amount in integer minor units
     * @param currency        ISO 4217 currency code
     * @param intervalDays    billing interval in days
     * @param trialDays       trial period in days (0 = no trial)
     * @param idempotencyKey  caller-supplied idempotency key
     * @return canonical result with providerSubscriptionId
     */
    ProviderSubscriptionResult createSubscription(
        UUID subscriptionId,
        long planAmount,
        String currency,
        int intervalDays,
        int trialDays,
        String idempotencyKey
    );

    /**
     * Cancels a provider subscription.
     *
     * @param providerSubscriptionId  provider's subscription identifier
     * @param cancelAtPeriodEnd       if true, cancel at end of current period
     * @param idempotencyKey          idempotency key for safe retries
     */
    void cancelSubscription(
        String providerSubscriptionId,
        boolean cancelAtPeriodEnd,
        String idempotencyKey
    );

    /**
     * Verifies a webhook payload signature.
     *
     * @param rawPayload       raw request body bytes
     * @param signatureHeader  value of the signature header
     * @return parsed event type (canonical)
     * @throws WebhookSignatureException if signature is invalid or timestamp is stale
     */
    WebhookEvent verifyAndParseWebhook(byte[] rawPayload, String signatureHeader);

    // ─── Result records ──────────────────────────────────────────────────────

    record ProviderSubscriptionResult(
        String providerSubscriptionId,
        String providerStatus  // canonical: "trialing", "active", etc.
    ) {}

    record WebhookEvent(
        String providerEventId,
        String eventType,        // canonical event type string
        String subscriptionRef,  // provider's subscription ID referenced in event
        String metadataJson      // normalized JSON metadata (no provider-specific fields)
    ) {}

    // ─── Exceptions ──────────────────────────────────────────────────────────

    class BillingProviderException extends RuntimeException {
        private final String provider;
        public BillingProviderException(String provider, String message, Throwable cause) {
            super("[" + provider + "] " + message, cause);
            this.provider = provider;
        }
        public String getProvider() { return provider; }
    }

    class WebhookSignatureException extends RuntimeException {
        public WebhookSignatureException(String message) { super(message); }
    }
}
