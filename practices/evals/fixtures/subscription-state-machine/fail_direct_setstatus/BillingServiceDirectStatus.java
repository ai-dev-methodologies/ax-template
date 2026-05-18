package ax.template.billing;

import ax.template.billing.Subscription;
import ax.template.billing.SubscriptionRepository;
import ax.template.billing.SubscriptionStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * FAILING FIXTURE — rule: subscription-state-machine-explicit
 *
 * Violation: service calls subscription.applyStatusTransition() directly,
 * bypassing SubscriptionStateMachine. No BillingEvent recorded. Counter not incremented.
 */
@Service
class BillingServiceDirectStatus {

    private final SubscriptionRepository subscriptionRepository;

    BillingServiceDirectStatus(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    // VIOLATION: direct applyStatusTransition() call outside SubscriptionStateMachine
    // No BillingEvent recorded. Transition validation skipped. Counter not incremented.
    void activateSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow();

        // ← VIOLATION: only SubscriptionStateMachine may call applyStatusTransition()
        subscription.applyStatusTransition(SubscriptionStatus.ACTIVE);

        subscriptionRepository.save(subscription);
        // Missing: BillingEvent(PAYMENT_SUCCEEDED)
        // Missing: billing.subscription.lifecycle_transition counter
    }
}
