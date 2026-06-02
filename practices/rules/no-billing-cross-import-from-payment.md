---
title: "billing and payment packages must not import each other; the boundary defined in §5.2.6 is enforced by ArchUnit"
rule_id: no-billing-cross-import-from-payment
impact: CRITICAL
impactDescription: "Cross-importing between billing and payment creates a circular bounded-context dependency. Any change to payment internals (e.g., PaymentMethod, PaymentStatus) leaks into billing and forces cascading changes. Subscription lifecycle (billing domain) must never depend on one-shot charge logic (payment domain)."
tags:
  - billing
  - payment
  - boundary
  - ddd
  - domain-separation
provenance_class: internal_design
protects_template_id: templates/backend/billing/BillingService.java
failing_fixture_path: practices/evals/fixtures/no-billing-cross-import-from-payment/fail_billing_imports_payment/
spec_ref: "specs/billing-l0.yaml#BILLING-BOUNDARY-001"
verification:
  gradle_task: testBilling
  tag: BILLING-BOUNDARY-001
  notes: |
    Enforced by BillingArchitectureTest.billingMustNotImportPayment +
    paymentMustNotImportBilling (@Tag BILLING-BOUNDARY-001).
    ArchUnit rules (two directional):
    noClasses().that().resideInAPackage("..billing..")
        .should().dependOnClassesThat().resideInAPackage("..payment..")
    noClasses().that().resideInAPackage("..payment..")
        .should().dependOnClassesThat().resideInAPackage("..billing..")
    Failing fixture: any billing class with import ax.template.payment.* or vice versa.
evidence:
  - source_type: external
    citation: "Domain-Driven Design (Evans): Each bounded context has an explicit contract at its boundary. Cross-importing internals couples contexts at the class level, violating autonomy and enabling cascading changes."
    url: "https://martinfowler.com/bliki/BoundedContext.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Stripe API Reference 2026-05 — Charges vs. Subscriptions are separate API resources with no direct dependency between them. A subscription's lifecycle uses invoice and billing objects, not charge objects."
    url: "https://stripe.com/docs/api/subscriptions"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Sam Newman — Building Microservices (2nd ed.): Services in separate bounded contexts must communicate via published events or APIs, never via direct class-level imports."
    url: "https://samnewman.io/books/building_microservices_2nd_edition/"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## billing ↔ payment cross-import is prohibited

**Impact: CRITICAL — billing domain (subscription lifecycle, invoices, recurring events) and payment domain (one-shot authorize/capture/refund) are separate bounded contexts per §5.2.6. Cross-imports couple contexts at the Java class level, breaking independent deployability and forcing cascading changes.**

### §5.2.6 Payment vs Billing Boundary

| Concern | Owner |
|---|---|
| One-shot authorize/capture/refund | `payment` domain |
| Subscription lifecycle | `billing` domain |
| Invoice issuance | `billing` domain |
| Plan management | `billing` domain |
| Recurring billing event normalization | `billing` domain |

Communication between the domains, if needed, must go through:
1. **Application events** (Spring `ApplicationEvent` or Kafka topic)
2. **Shared kernel** types only (primitives, common value objects in a `shared` package)

Direct Java `import ax.template.payment.*` or `import ax.template.billing.*` from the opposing context is **prohibited**.

**Incorrect — billing imports payment internals (cross-context dependency):**

```java
// VIOLATION: billing service directly importing payment domain class
package ax.template.billing;

import ax.template.payment.PaymentMethod;      // ← VIOLATION
import ax.template.payment.PaymentService;     // ← VIOLATION

@Service
public class BillingService {
    private final PaymentService paymentService;
    public void renewSubscription(UUID subId) {
        paymentService.charge(...); // cross-context direct call — forbidden
    }
}
```

**Correct — billing domain coordinates via ApplicationEvent, no payment imports:**

```java
// CORRECT: billing emits an event; payment coordinator listens (no payment.* import)
package ax.template.billing;

@Service
public class BillingService {
    private final ApplicationEventPublisher events;
    @Transactional
    public void handleRenewal(UUID subscriptionId) {
        events.publishEvent(new SubscriptionRenewalDueEvent(subscriptionId, amountDue));
        // No payment import needed — payment domain handles via its own listener
    }
}
```

Reference: https://martinfowler.com/bliki/BoundedContext.html

### Incorrect — payment imports billing internals

```java
// VIOLATION: payment domain importing billing domain class
package ax.template.payment;

import ax.template.billing.Subscription;       // ← VIOLATION
import ax.template.billing.BillingEvent;       // ← VIOLATION

@Service
public class PaymentService {
    public void processRefund(UUID subId) {
        // Should not know about Subscription entity
        Subscription sub = subscriptionRepository.findById(subId);
    }
}
```

### Correct — event-driven coordination

```java
// CORRECT: billing emits an event; payment (or a coordinator) listens
// billing domain:
@Service
public class BillingService {
    private final ApplicationEventPublisher events;

    @Transactional
    public void handleSubscriptionRenewal(UUID subscriptionId) {
        // ... state machine transition ...
        events.publishEvent(new SubscriptionRenewalDueEvent(subscriptionId, amountDue));
        // No payment import needed
    }
}

// Coordinator (shared layer or separate service) — NOT in billing or payment:
@Component
public class RenewalCoordinator {
    @EventListener
    public void onRenewalDue(SubscriptionRenewalDueEvent event) {
        // Calls payment domain via its API, not its internals
        paymentGateway.charge(event.subscriptionId(), event.amountDue());
    }
}
```

### Correct — shared kernel for common types only

```java
// shared package (not billing, not payment) — OK to import from either context:
package ax.template.shared;

public record MoneyAmount(long amount, String currency) {}
public record UserId(UUID value) {}
```

## ArchUnit enforcement

```java
// BillingArchitectureTest.java
@ArchTest
static final ArchRule billingMustNotImportPayment = noClasses()
    .that().resideInAPackage("..billing..")
    .should().dependOnClassesThat().resideInAPackage("..payment..")
    .because("billing and payment are separate bounded contexts (§5.2.6)");

@ArchTest
static final ArchRule paymentMustNotImportBilling = noClasses()
    .that().resideInAPackage("..payment..")
    .should().dependOnClassesThat().resideInAPackage("..billing..")
    .because("billing and payment are separate bounded contexts (§5.2.6)");
```

## Failing fixture

See: `practices/evals/fixtures/no-billing-cross-import-from-payment/fail_billing_imports_payment/BillingServiceCrossImport.java` — a billing service that imports `ax.template.payment.PaymentService`.

See: `practices/evals/fixtures/no-billing-cross-import-from-payment/pass_idempotency_pattern_no_import/BillingServiceNoPaymentImport.java` — correct billing service with no payment imports.
