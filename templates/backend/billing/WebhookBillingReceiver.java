/**
 * @ax-template-meta
 * template_id: backend/billing/WebhookBillingReceiver
 * layer: backend-domain
 * domain: billing
 * anchors_rule: billing-event-idempotent.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Webhook signature verification"
 *     quote: "To protect against replay attacks, Stripe includes a timestamp in the Stripe-Signature header."
 *   - source_type: upstream_id
 *     upstream_id: toss-billing-2026-05
 *     section: "웹훅 이벤트"
 *     quote: "X-Webhook-Signature 헤더에 HMAC-SHA256 서명이 포함됨."
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Webhook endpoint does NOT require @RequireIdempotencyKey (idempotency handled by BillingEvent.idempotencyKey unique constraint).
 *   Signature verification happens in BillingProvider.verifyAndParseWebhook() before any state change.
 *   Boundary: WebhookBillingReceiver is in billing domain. Never import from payment domain.
 */
package com.example.app.billing;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebhookBillingReceiver — handles incoming billing provider webhooks.
 *
 * <p>Security model:
 * <ol>
 *   <li>Signature verified by {@link BillingProvider#verifyAndParseWebhook} before processing.
 *       Invalid signature or stale timestamp → HTTP 401 (BILLING-WEBHOOK-001, BILLING-IDEMP-002).
 *   <li>Idempotency enforced by unique {@code BillingEvent.idempotencyKey} constraint.
 *       Duplicate provider event → DB constraint violation → HTTP 409 Conflict.
 * </ol>
 *
 * <p>No authentication header required (provider calls this endpoint directly).
 * CSRF protection disabled for this endpoint via SecurityConfig.
 *
 * <p>Boundary: WebhookBillingReceiver is in billing domain. No import from payment domain.
 */
@RestController
@RequestMapping("/api/webhooks/billing")
public class WebhookBillingReceiver {

    private static final Logger log = LoggerFactory.getLogger(WebhookBillingReceiver.class);

    private final BillingProvider billingProvider;
    private final SubscriptionRepository subscriptionRepository;
    private final BillingEventRepository billingEventRepository;
    private final SubscriptionStateMachine stateMachine;
    private final MeterRegistry meterRegistry;

    public WebhookBillingReceiver(
            BillingProvider billingProvider,
            SubscriptionRepository subscriptionRepository,
            BillingEventRepository billingEventRepository,
            SubscriptionStateMachine stateMachine,
            MeterRegistry meterRegistry) {
        this.billingProvider = billingProvider;
        this.subscriptionRepository = subscriptionRepository;
        this.billingEventRepository = billingEventRepository;
        this.stateMachine = stateMachine;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Receives billing webhook. Accepts both Stripe and Toss events.
     *
     * <p>Provider is determined by the adapter registered in the Spring context.
     * For multi-provider setups, use separate URL paths (/stripe, /toss) or
     * a routing header.
     *
     * @param rawPayload       raw request body bytes (preserved for signature verification)
     * @param stripeSignature  Stripe-Signature header (null for Toss)
     * @param tossSignature    X-Webhook-Signature header (null for Stripe)
     */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody byte[] rawPayload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String tossSignature) {

        String signatureHeader = stripeSignature != null ? stripeSignature : tossSignature;
        if (signatureHeader == null) {
            log.warn("Webhook received without any signature header");
            return ResponseEntity.status(401).build();
        }

        // 1. Verify signature and parse canonical event.
        BillingProvider.WebhookEvent event;
        try {
            event = billingProvider.verifyAndParseWebhook(rawPayload, signatureHeader);
        } catch (BillingProvider.WebhookSignatureException e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }

        meterRegistry.counter("billing.webhook.received_count",
            "provider", billingProvider.providerName(),
            "event_type", event.eventType()
        ).increment();

        // 2. Route to state machine based on canonical event type.
        if (event.subscriptionRef() == null) {
            log.debug("Webhook event {} has no subscriptionRef; skipping state transition", event.eventType());
            return ResponseEntity.ok().build();
        }

        var subOpt = subscriptionRepository.findByProviderSubscriptionId(event.subscriptionRef());
        if (subOpt.isEmpty()) {
            log.warn("Webhook received for unknown providerSubscriptionId={}", event.subscriptionRef());
            return ResponseEntity.ok().build(); // 200 to prevent provider retries for unknown subs
        }
        Subscription sub = subOpt.get();

        SubscriptionStateMachine.Trigger trigger = mapToTrigger(event.eventType());
        if (trigger != null) {
            try {
                BillingEvent billingEvent = stateMachine.transition(sub, trigger, event.metadataJson());
                // Check idempotency — duplicate providerEventId → no second row
                log.info("Webhook trigger={} subscriptionId={} billingEventId={}", trigger, sub.getId(), billingEvent.getId());
            } catch (SubscriptionStateMachine.InvalidTransitionException e) {
                log.warn("Invalid webhook transition: {}", e.getMessage());
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("unique constraint")) {
                    meterRegistry.counter("billing.event.idempotency_hit_count").increment();
                    log.info("Duplicate billing event detected (idempotencyKey={}); skipping.", event.providerEventId());
                    return ResponseEntity.ok().build();
                }
                throw e;
            }
        }

        return ResponseEntity.ok().build();
    }

    private SubscriptionStateMachine.Trigger mapToTrigger(String canonicalEventType) {
        return switch (canonicalEventType) {
            case "PAYMENT_SUCCEEDED"   -> SubscriptionStateMachine.Trigger.PAYMENT_SUCCEEDED_WEBHOOK;
            case "PAYMENT_FAILED"      -> SubscriptionStateMachine.Trigger.PAYMENT_FAILED_WEBHOOK;
            case "TRIAL_END"           -> SubscriptionStateMachine.Trigger.TRIAL_END_WEBHOOK;
            case "SUBSCRIPTION_CANCELLED" -> SubscriptionStateMachine.Trigger.UNPAID_THRESHOLD_EXCEEDED;
            default                    -> null;
        };
    }
}
