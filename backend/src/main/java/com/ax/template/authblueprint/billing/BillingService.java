package com.ax.template.authblueprint.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * R21 billing domain service.
 * <p>Trace:
 * <ul>
 *   <li>BILLING-AUTHZ-002 — {@link #findOwn(String, String)} is owner-scoped.</li>
 *   <li>BILLING-IDEMP-001 — {@link #recordWebhookOnce} de-dupes on provider_event_id.</li>
 *   <li>BILLING-STATE-001/002 — only path that mutates Subscription.status is
 *       {@link #applyTransition}, which delegates to {@link SubscriptionStateMachine}.</li>
 * </ul>
 * <p>BILLING-BOUNDARY-001: this class does not import any
 * {@code com.ax.template.authblueprint.payment.*} symbol. The one-shot
 * payment domain handles authorize/capture/refund; billing handles
 * subscription lifecycle. Boundary enforced by
 * {@code BillingPaymentBoundaryTest}.
 */
@Service
public class BillingService {

    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final BillingEventRepository events;
    private final SubscriptionStateMachine stateMachine;

    public BillingService(SubscriptionRepository subscriptions,
                          PlanRepository plans,
                          BillingEventRepository events,
                          SubscriptionStateMachine stateMachine) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.events = events;
        this.stateMachine = stateMachine;
    }

    // ─── Plan management (admin) ───────────────────────────────────────────────

    @Transactional
    public Plan createPlan(String name, long amount, String currency, BillingCycle cycle) {
        return plans.save(Plan.create(name, amount, currency, cycle));
    }

    public Page<Plan> listPlans(Pageable pageable) {
        return plans.findAllByDeletedAtIsNull(pageable);
    }

    // ─── Subscription lifecycle ────────────────────────────────────────────────

    @Transactional
    public Subscription createSubscription(String userId, String planId, String provider) {
        Plan plan = plans.findById(planId)
            .orElseThrow(() -> new BillingException.PlanNotFound(planId));
        Subscription sub = Subscription.createTrial(userId, plan, provider);
        return subscriptions.save(sub);
    }

    public Page<Subscription> listOwn(String userId, Pageable pageable) {
        return subscriptions.findAllByUserIdAndDeletedAtIsNull(userId, pageable);
    }

    /** BILLING-AUTHZ-002 — IDOR-safe; cross-user lookup returns {@code empty()}. */
    public Optional<Subscription> findOwn(String id, String userId) {
        return subscriptions.findByIdAndUserId(id, userId);
    }

    @Transactional
    public Subscription cancelOwn(String id, String userId) {
        Subscription sub = subscriptions.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BillingException.SubscriptionNotFound(id));
        applyTransition(sub, SubscriptionStatus.CANCELLED, BillingEventType.SUBSCRIPTION_CANCELLED,
            "internal-cancel-" + sub.getId(), sub.getProvider(), null);
        return sub;
    }

    // ─── Webhook intake (BILLING-IDEMP-001 + BILLING-STATE-002) ────────────────

    /**
     * BILLING-IDEMP-001 — idempotent on {@code providerEventId}. If a row with the
     * same id already exists, returns it without invoking the supplier (no
     * state mutation, no double-processing).
     */
    @Transactional
    public BillingEvent recordWebhookOnce(String providerEventId, String provider,
                                          BillingEventType type, String subscriptionId,
                                          String payload) {
        Optional<BillingEvent> existing = events.findByProviderEventId(providerEventId);
        if (existing.isPresent()) {
            return existing.get();
        }
        return events.save(BillingEvent.of(providerEventId, subscriptionId, type, provider, payload));
    }

    /**
     * BILLING-STATE-002 — apply a legal transition and append a BillingEvent
     * ledger row in the same transaction. Caller has already verified the
     * webhook signature and de-duplicated on event_id.
     */
    @Transactional
    public void applyTransition(Subscription sub, SubscriptionStatus next,
                                BillingEventType type, String providerEventId,
                                String provider, String payload) {
        stateMachine.transition(sub, next);
        subscriptions.save(sub);
        // Append-only ledger. Idempotency-checked on call sites that come from
        // webhooks; internal-cancel paths use a synthetic event id.
        if (events.findByProviderEventId(providerEventId).isEmpty()) {
            events.save(BillingEvent.of(providerEventId, sub.getId(), type, provider, payload));
        }
    }
}
