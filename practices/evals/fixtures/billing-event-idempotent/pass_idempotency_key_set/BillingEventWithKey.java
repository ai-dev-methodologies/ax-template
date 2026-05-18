package ax.template.billing;

import ax.template.billing.BillingEvent;
import ax.template.billing.BillingEventType;
import ax.template.billing.BillingEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

/**
 * PASSING FIXTURE — rule: billing-event-idempotent
 *
 * Correct: BillingEvent.fromWebhook() sets idempotencyKey from provider event ID.
 * Duplicate webhook with same providerEventId → DataIntegrityViolationException → 200.
 */
@Service
class BillingEventWithKeyService {

    private final BillingEventRepository billingEventRepository;

    BillingEventWithKeyService(BillingEventRepository billingEventRepository) {
        this.billingEventRepository = billingEventRepository;
    }

    // CORRECT: fromWebhook() sets idempotencyKey = providerEventId
    // Duplicate webhook replay → unique constraint violation → 200 (no double transition)
    boolean recordPaymentSucceeded(UUID subscriptionId, String providerEventId, Instant occurredAt, String metadataJson) {
        try {
            BillingEvent event = BillingEvent.fromWebhook(
                subscriptionId,
                BillingEventType.PAYMENT_SUCCEEDED,
                providerEventId,          // idempotencyKey = stripe evt_xxx or toss payment_xxx
                providerEventId,
                occurredAt,
                metadataJson
            );
            billingEventRepository.save(event);
            return true; // new event
        } catch (DataIntegrityViolationException e) {
            // Duplicate webhook — idempotency hit — return true (already processed)
            return false;
        }
    }
}
