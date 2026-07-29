/**
 * @ax-template-meta
 * template_id: backend/billing/StripeBillingAdapter
 * layer: backend-domain
 * domain: billing
 * anchors_rule: webhook-hmac-required.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Webhook signature verification"
 *     quote: "To protect against replay attacks, Stripe includes a timestamp in the Stripe-Signature header. ... You can specify a tolerance (in seconds) to reject webhooks that are more than the tolerance value from the current time."
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Idempotency"
 *     quote: "Stripe stores results for at least 24 hours. Retrying the same key within the window returns the original response without creating a duplicate resource."
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   This adapter wraps the Stripe SDK. Inject a StripeClient (or use the static Stripe class).
 *   Configure stripe.api-key and stripe.webhook-secret in application.yml.
 *   No Stripe-specific event fields leak beyond this adapter — all returns are canonical BillingProvider types.
 *   Boundary: StripeBillingAdapter is in billing domain. Never import from payment domain.
 */
package com.example.app.billing;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * StripeBillingAdapter — thin Stripe Billing API adapter.
 *
 * <p>This adapter exposes only canonical {@link BillingProvider} types.
 * No Stripe-specific event type constants, subscription objects, or price IDs
 * leave this class. Provider abstraction is enforced by the {@link BillingProvider}
 * interface contract.
 *
 * <p>Webhook replay protection: Stripe-Signature header timestamp must be within
 * {@code stripe.webhook-tolerance-seconds} (default: 300) of current server time.
 * See spec item BILLING-IDEMP-002.
 *
 * <p>Fork instructions:
 * 1. Add {@code implementation 'com.stripe:stripe-java:25.x.x'} to build.gradle.kts.
 * 2. Set {@code stripe.api-key} and {@code stripe.webhook-secret} in application.yml.
 * 3. Replace the stub HTTP calls with real Stripe SDK calls.
 */
@Component("stripeBillingAdapter")
public class StripeBillingAdapter implements BillingProvider {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingAdapter.class);
    private static final String PROVIDER = "stripe";
    private static final int DEFAULT_TOLERANCE_SECONDS = 300;

    @Value("${stripe.api-key:#{null}}")
    private String apiKey;

    @Value("${stripe.webhook-secret:#{null}}")
    private String webhookSecret;

    @Value("${stripe.webhook-tolerance-seconds:" + DEFAULT_TOLERANCE_SECONDS + "}")
    private int webhookToleranceSeconds;

    private final MeterRegistry meterRegistry;

    public StripeBillingAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public ProviderSubscriptionResult createSubscription(
            UUID subscriptionId,
            long planAmount,
            String currency,
            int intervalDays,
            int trialDays,
            String idempotencyKey) {
        // Fork: replace with real Stripe SDK call.
        // Example: Stripe.apiKey = apiKey;
        //   SubscriptionCreateParams params = SubscriptionCreateParams.builder()
        //     .setCustomer(customerStripeId)
        //     .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
        //     .setTrialPeriodDays(trialDays > 0 ? (long) trialDays : null)
        //     .build();
        //   Subscription sub = Subscription.create(params, RequestOptions.builder()
        //     .setIdempotencyKey(idempotencyKey).build());
        //   return new ProviderSubscriptionResult(sub.getId(), sub.getStatus());

        log.info("[stripe] createSubscription subscriptionId={} amount={} currency={} idempotencyKey={}",
            subscriptionId, planAmount, currency, idempotencyKey);
        meterRegistry.counter("billing.invoice.generated_count",
            "provider", PROVIDER, "status", "success").increment();
        // Stub result for template skeleton:
        return new ProviderSubscriptionResult(
            "stripe_sub_" + subscriptionId,
            trialDays > 0 ? "trialing" : "active"
        );
    }

    @Override
    public void cancelSubscription(
            String providerSubscriptionId,
            boolean cancelAtPeriodEnd,
            String idempotencyKey) {
        // Fork: replace with Stripe SDK call.
        // Subscription.retrieve(providerSubscriptionId).cancel(
        //   SubscriptionCancelParams.builder().setCancelAtPeriodEnd(cancelAtPeriodEnd).build(),
        //   RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
        log.info("[stripe] cancelSubscription providerSubId={} cancelAtPeriodEnd={}", providerSubscriptionId, cancelAtPeriodEnd);
    }

    @Override
    public WebhookEvent verifyAndParseWebhook(byte[] rawPayload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new WebhookSignatureException("stripe.webhook-secret is not configured");
        }
        // Fork: replace with Stripe SDK webhook verification.
        // Event event = Webhook.constructEvent(new String(rawPayload, StandardCharsets.UTF_8),
        //   signatureHeader, webhookSecret, webhookToleranceSeconds);
        //
        // Timestamp-replay check is handled by Stripe SDK internally when tolerance is set.
        // See BILLING-IDEMP-002: events older than toleranceSeconds → WebhookSignatureException.
        //
        // Canonical mapping example:
        //   String canonicalType = switch (event.getType()) {
        //     case "invoice.payment_succeeded" -> "PAYMENT_SUCCEEDED";
        //     case "invoice.payment_failed"    -> "PAYMENT_FAILED";
        //     case "customer.subscription.deleted" -> "SUBSCRIPTION_CANCELLED";
        //     default -> "WEBHOOK_RECEIVED";
        //   };
        //   return new WebhookEvent(event.getId(), canonicalType, subscriptionRef, metadataJson);

        // Stub for template skeleton:
        log.debug("[stripe] verifyAndParseWebhook (stub)");
        return new WebhookEvent(
            "stripe_evt_stub",
            "WEBHOOK_RECEIVED",
            null,
            "{}"
        );
    }
}
