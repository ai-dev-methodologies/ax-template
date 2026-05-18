package ax.template.billing;

// VIOLATION: billing domain importing payment domain classes
import ax.template.payment.PaymentService;       // ← VIOLATION
import ax.template.payment.PaymentMethod;        // ← VIOLATION
import ax.template.payment.PaymentStatus;        // ← VIOLATION

import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * FAILING FIXTURE — rule: no-billing-cross-import-from-payment
 *
 * Violation: billing service directly imports payment domain classes.
 * Any change to PaymentService/PaymentMethod/PaymentStatus cascades into billing.
 * ArchUnit: noClasses in ..billing.. should depend on classes in ..payment..
 */
@Service
class BillingServiceCrossImport {

    // VIOLATION: PaymentService injected into billing — cross-context coupling
    private final PaymentService paymentService;

    BillingServiceCrossImport(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    void renewSubscription(UUID subscriptionId) {
        // VIOLATION: billing calling payment service directly
        PaymentMethod method = paymentService.getDefaultMethod(subscriptionId);
        if (method.getStatus() == PaymentStatus.ACTIVE) {
            paymentService.charge(subscriptionId, 10000L);
        }
    }
}
