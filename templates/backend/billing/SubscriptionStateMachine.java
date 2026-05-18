/**
 * @ax-template-meta
 * template_id: backend/billing/SubscriptionStateMachine
 * layer: backend-domain
 * domain: billing
 * anchors_rule: subscription-state-machine-explicit.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Subscription lifecycle"
 *     quote: "trialing → active → past_due → canceled"
 *   - source_type: upstream_id
 *     upstream_id: toss-billing-2026-05
 *     section: "빌링키 상태"
 *     quote: "ACTIVE: 정상 사용 가능, INACTIVE: 카드 만료/분실 등으로 비활성화"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   SubscriptionStateMachine is the SOLE class allowed to mutate Subscription.status.
 *   All callers (BillingService, WebhookBillingReceiver) must go through this class.
 *   Enforced by ArchUnit: OnlyStateMachineMutatesSubscriptionStatusArchTest.
 */
package com.example.app.billing;

import com.example.app.billing.Subscription.SubscriptionStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * SubscriptionStateMachine — sole authority for Subscription status transitions.
 *
 * <p>Only this class may call {@code Subscription.applyStatusTransition()}.
 * Service and webhook classes must call {@link #transition(Subscription, Trigger)} instead.
 *
 * <p>Every transition records a {@link BillingEvent} (append-only).
 * Observability: {@code billing.subscription.lifecycle_transition} counter is incremented.
 *
 * <p>Allowed transitions (see also {@code blueprints/billing-manifest.yaml#state_machine}):
 * <ul>
 *   <li>TRIAL → ACTIVE (trigger: TRIAL_END_WEBHOOK)</li>
 *   <li>TRIAL → CANCELLED (trigger: USER_CANCEL)</li>
 *   <li>ACTIVE → PAST_DUE (trigger: PAYMENT_FAILED_WEBHOOK)</li>
 *   <li>ACTIVE → ACTIVE (trigger: PAYMENT_SUCCEEDED_WEBHOOK — renew period)</li>
 *   <li>ACTIVE → CANCELLED (trigger: USER_CANCEL)</li>
 *   <li>PAST_DUE → ACTIVE (trigger: PAYMENT_SUCCEEDED_WEBHOOK)</li>
 *   <li>PAST_DUE → CANCELLED (trigger: UNPAID_THRESHOLD_EXCEEDED)</li>
 * </ul>
 */
@Component
public class SubscriptionStateMachine {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionStateMachine.class);

    private final BillingEventRepository billingEventRepository;
    private final MeterRegistry meterRegistry;

    public SubscriptionStateMachine(
            BillingEventRepository billingEventRepository,
            MeterRegistry meterRegistry) {
        this.billingEventRepository = billingEventRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Triggers a state transition on the subscription.
     *
     * @param subscription the subscription to transition (must be managed JPA entity)
     * @param trigger      the event trigger driving the transition
     * @param metadata     optional JSON metadata for the resulting BillingEvent
     * @return the BillingEvent recorded for this transition
     * @throws InvalidTransitionException if the trigger is not valid for the current status
     */
    @Transactional
    public BillingEvent transition(Subscription subscription, Trigger trigger, String metadata) {
        SubscriptionStatus from = subscription.getStatus();
        TransitionResult result = TRANSITIONS.get(new StateKey(from, trigger));
        if (result == null) {
            throw new InvalidTransitionException(
                "No valid transition from " + from + " on trigger " + trigger);
        }

        SubscriptionStatus to = result.targetStatus();
        subscription.applyStatusTransition(to);
        if (trigger == Trigger.PAYMENT_SUCCEEDED_WEBHOOK && to == SubscriptionStatus.ACTIVE) {
            subscription.advanceBillingPeriod();
        }

        BillingEvent event = BillingEvent.createInternal(
            subscription.getId(),
            result.eventType(),
            metadata
        );
        billingEventRepository.save(event);

        meterRegistry.counter("billing.subscription.lifecycle_transition",
            "from", from.name(),
            "to", to.name(),
            "trigger", trigger.name()
        ).increment();

        log.info("Subscription {} transitioned {} → {} (trigger={})",
            subscription.getId(), from, to, trigger);

        return event;
    }

    @Transactional
    public BillingEvent transition(Subscription subscription, Trigger trigger) {
        return transition(subscription, trigger, null);
    }

    // ─── Trigger enum ─────────────────────────────────────────────────────────

    public enum Trigger {
        TRIAL_END_WEBHOOK,
        PAYMENT_SUCCEEDED_WEBHOOK,
        PAYMENT_FAILED_WEBHOOK,
        USER_CANCEL,
        UNPAID_THRESHOLD_EXCEEDED
    }

    // ─── Internal transition table ────────────────────────────────────────────

    private record StateKey(SubscriptionStatus from, Trigger trigger) {}
    private record TransitionResult(SubscriptionStatus targetStatus,
                                    BillingEvent.BillingEventType eventType) {}

    private static final Map<StateKey, TransitionResult> TRANSITIONS = Map.of(
        new StateKey(SubscriptionStatus.TRIAL, Trigger.TRIAL_END_WEBHOOK),
            new TransitionResult(SubscriptionStatus.ACTIVE, BillingEvent.BillingEventType.TRIAL_END),
        new StateKey(SubscriptionStatus.TRIAL, Trigger.USER_CANCEL),
            new TransitionResult(SubscriptionStatus.CANCELLED, BillingEvent.BillingEventType.SUBSCRIPTION_CANCELLED),

        new StateKey(SubscriptionStatus.ACTIVE, Trigger.PAYMENT_FAILED_WEBHOOK),
            new TransitionResult(SubscriptionStatus.PAST_DUE, BillingEvent.BillingEventType.PAYMENT_FAILED),
        new StateKey(SubscriptionStatus.ACTIVE, Trigger.PAYMENT_SUCCEEDED_WEBHOOK),
            new TransitionResult(SubscriptionStatus.ACTIVE, BillingEvent.BillingEventType.SUBSCRIPTION_RENEWED),
        new StateKey(SubscriptionStatus.ACTIVE, Trigger.USER_CANCEL),
            new TransitionResult(SubscriptionStatus.CANCELLED, BillingEvent.BillingEventType.SUBSCRIPTION_CANCELLED),

        new StateKey(SubscriptionStatus.PAST_DUE, Trigger.PAYMENT_SUCCEEDED_WEBHOOK),
            new TransitionResult(SubscriptionStatus.ACTIVE, BillingEvent.BillingEventType.PAYMENT_SUCCEEDED),
        new StateKey(SubscriptionStatus.PAST_DUE, Trigger.UNPAID_THRESHOLD_EXCEEDED),
            new TransitionResult(SubscriptionStatus.CANCELLED, BillingEvent.BillingEventType.SUBSCRIPTION_CANCELLED),
        new StateKey(SubscriptionStatus.PAST_DUE, Trigger.USER_CANCEL),
            new TransitionResult(SubscriptionStatus.CANCELLED, BillingEvent.BillingEventType.SUBSCRIPTION_CANCELLED)
    );

    // ─── Exception ────────────────────────────────────────────────────────────

    public static class InvalidTransitionException extends RuntimeException {
        public InvalidTransitionException(String message) {
            super(message);
        }
    }
}
