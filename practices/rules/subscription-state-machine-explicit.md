---
title: "Subscription.status must only be mutated through SubscriptionStateMachine; direct setStatus() calls outside the state machine are prohibited"
rule_id: subscription-state-machine-explicit
impact: CRITICAL
impactDescription: "Direct setStatus() calls bypass the state machine's transition validation and BillingEvent recording, creating silent state corruption and missing audit trail entries"
tags:
  - billing
  - state-machine
  - subscription
  - audit
provenance_class: internal_design
protects_template_id: templates/backend/billing/Subscription.java
failing_fixture_path: practices/evals/fixtures/subscription-state-machine/fail_direct_setstatus/
spec_ref: "specs/billing-l0.yaml#BILLING-STATE-001"
verification:
  gradle_task: testBilling
  tag: BILLING-STATE-001
  notes: |
    Enforced by BillingArchitectureTest.onlyStateMachineMutatesSubscriptionStatus
    (@Tag BILLING-STATE-001): no class in ..billing.. other than SubscriptionStateMachine
    (or Subscription itself) may call Subscription.setStatus(SubscriptionStatus) — the
    package-private status mutator that gates every lifecycle transition.
    Failing fixture: any other billing class calling setStatus(...).
evidence:
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Subscription lifecycle (status table)"
    quote: "trialing The subscription is currently in a trial period and you can safely provision your product for your customer. The subscription transitions automatically to active when a customer makes the first payment."
  - source_type: upstream_id
    upstream_id: toss-billing-2026-05
    section: "구독 상태는 가맹점이 직접 관리 (Toss는 상태 enum 미제공)"
    quote: "구독 서비스는 API를 사용해서 직접 구축해야 합니다."
  - source_type: external
    anchors: generic_principle_only
    citation: "Domain-Driven Design — Aggregates encapsulate invariants; state transitions are explicit methods on the aggregate, not raw field mutations"
    url: "https://martinfowler.com/bliki/DDD_Aggregate.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Subscription.status must only be mutated through SubscriptionStateMachine

**Impact: CRITICAL — Calling `subscription.setStatus()` directly from service code bypasses transition validation, skips BillingEvent recording, and leaves the audit trail incomplete. Subscription state becomes inconsistent with billing events.**

The `SubscriptionStateMachine` is the sole class responsible for:
1. Validating whether a transition is allowed (TRIAL→PAST_DUE is invalid; PAST_DUE→ACTIVE is valid).
2. Calling `Subscription.setStatus()` (package-private method).
3. Recording a `BillingEvent` for the transition (append-only audit trail).
4. Emitting `billing.subscription.lifecycle_transition` counter.

Any code that mutates `Subscription.status` outside this machine will:
- Skip transition validation (allowing impossible states like CANCELLED→ACTIVE without payment).
- Leave no BillingEvent audit record (compliance and debugging impact).
- Cause observability counters to miss transitions.

**Incorrect — direct setStatus() outside SubscriptionStateMachine:**

```java
// VIOLATION: direct mutation bypasses validation and BillingEvent recording
subscription.setStatus(SubscriptionStatus.ACTIVE);
subscriptionRepository.save(subscription);
// No BillingEvent recorded. Transition validation skipped. Counter not incremented.
```

**Correct — all state transitions through SubscriptionStateMachine.transition():**

```java
// CORRECT: all state transitions through the state machine
BillingEvent event = stateMachine.transition(
    subscription,
    SubscriptionStateMachine.Trigger.PAYMENT_SUCCEEDED_WEBHOOK,
    webhookMetadataJson
);
// Validates PAST_DUE→ACTIVE transition.
// Saves BillingEvent(PAYMENT_SUCCEEDED, idempotencyKey=...).
// Increments billing.subscription.lifecycle_transition counter.
```

Reference: https://martinfowler.com/bliki/DDD_Aggregate.html

## ArchUnit enforcement

```java
// OnlyStateMachineMutatesSubscriptionStatusArchTest.java
@ArchTest
static final ArchRule onlyStateMachineMutatesStatus = noClasses()
    .that().areNotAssignableTo(SubscriptionStateMachine.class)
    .should().callMethodWhere(
        target().hasName("setStatus")
            .and(owner().isAssignableTo(Subscription.class))
    )
    .because("Subscription status may only be changed via SubscriptionStateMachine");
```

## Failing fixture

See: `practices/evals/fixtures/subscription-state-machine/fail_direct_setstatus/BillingServiceDirectStatus.java` — a service method that calls `subscription.setStatus()` directly.
