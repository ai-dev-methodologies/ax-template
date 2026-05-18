package ax.template.billing;

import ax.template.billing.BillingEvent;
import ax.template.billing.Subscription;
import ax.template.billing.SubscriptionRepository;
import ax.template.billing.SubscriptionStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * PASSING FIXTURE — rule: subscription-state-machine-explicit
 *
 * Correct: all state transitions go through SubscriptionStateMachine.transition().
 * BillingEvent recorded. Transition validated. Counter incremented.
 */
@Service
class BillingServiceStateMachine {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionStateMachine stateMachine;

    BillingServiceStateMachine(
        SubscriptionRepository subscriptionRepository,
        SubscriptionStateMachine stateMachine
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.stateMachine = stateMachine;
    }

    // CORRECT: transition goes through SubscriptionStateMachine
    // Validates PAST_DUE→ACTIVE. Records BillingEvent. Increments counter.
    @Transactional
    void handlePaymentSucceeded(UUID subscriptionId, String providerEventId, String metadataJson) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow();

        BillingEvent event = stateMachine.transition(
            subscription,
            SubscriptionStateMachine.Trigger.PAYMENT_SUCCEEDED_WEBHOOK,
            metadataJson
        );
        // stateMachine.transition() calls:
        //   1. subscription.applyStatusTransition(ACTIVE) — package-private, only callable by state machine
        //   2. billingEventRepository.save(BillingEvent(PAYMENT_SUCCEEDED, idempotencyKey=providerEventId))
        //   3. meterRegistry.counter("billing.subscription.lifecycle_transition", ...).increment()
    }
}
