package ax.template.billing;

import ax.template.billing.BillingEvent;
import ax.template.billing.BillingEventType;
import ax.template.billing.BillingEventRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * FAILING FIXTURE — rule: billing-event-idempotent
 *
 * Violation: BillingEvent created via raw constructor without idempotencyKey.
 * Duplicate webhook delivery creates a second row → double state transition.
 */
@Service
class BillingEventNoKeyService {

    private final BillingEventRepository billingEventRepository;

    BillingEventNoKeyService(BillingEventRepository billingEventRepository) {
        this.billingEventRepository = billingEventRepository;
    }

    // VIOLATION: BillingEvent constructed without idempotencyKey
    // Duplicate webhook creates second row → double PAYMENT_SUCCEEDED state transition
    void recordPaymentSucceeded(UUID subscriptionId, String providerEventId) {
        BillingEvent event = new BillingEvent(); // ← VIOLATION: no-arg constructor
        event.setSubscriptionId(subscriptionId);
        event.setEventType(BillingEventType.PAYMENT_SUCCEEDED);
        // idempotencyKey NOT set → null → DB allows duplicate on replay
        billingEventRepository.save(event);
    }
}
