package ax.template.billing;

// CORRECT: only billing domain imports — no payment.* imports
import ax.template.billing.Subscription;
import ax.template.billing.SubscriptionRepository;
import ax.template.billing.SubscriptionStateMachine;
import ax.template.billing.BillingEvent;
import ax.template.billing.BillingEventRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * PASSING FIXTURE — rule: no-billing-cross-import-from-payment
 *
 * Correct: billing service uses only billing domain imports.
 * Payment coordination happens via ApplicationEvent (no direct import needed).
 * ArchUnit: no ..payment.. dependency from ..billing..
 */
@Service
class BillingServiceNoPaymentImport {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionStateMachine stateMachine;
    private final ApplicationEventPublisher events;

    BillingServiceNoPaymentImport(
        SubscriptionRepository subscriptionRepository,
        SubscriptionStateMachine stateMachine,
        ApplicationEventPublisher events
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.stateMachine = stateMachine;
        this.events = events;
    }

    // CORRECT: no payment.* imports; coordination via event publishing
    @Transactional
    void initiateRenewal(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow();

        // Publish event — payment domain listener handles charge (separate context)
        events.publishEvent(new SubscriptionRenewalDueEvent(subscriptionId, subscription.getPlan().getAmount()));
        // No PaymentService, PaymentMethod, or PaymentStatus imports needed
    }

    // Domain event (defined in billing or shared package — never in payment)
    record SubscriptionRenewalDueEvent(UUID subscriptionId, long amountMinorUnits) {}
}
