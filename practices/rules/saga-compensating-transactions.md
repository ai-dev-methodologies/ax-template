---
title: A multi-service business transaction MUST be a saga — ordered local transactions with reverse-order compensation, never a distributed 2PC
impact: HIGH
impactDescription: "A business operation that spans services (place order → reserve credit → reserve inventory) cannot use one ACID transaction across databases. Doing the steps with no compensation means a failure at step 3 leaves steps 1–2 committed and inconsistent — credit reserved for an order that has no inventory, with nothing to undo it. The saga makes the sequence recoverable: each step is a local transaction, and a failure triggers compensating transactions that undo the prior steps in reverse order."
tags:
  - saga
  - compensating-transaction
  - distributed-transaction
  - orchestration
  - eventual-consistency
  - reliability
spec_ref: "specs/saga-orchestration-l0.yaml#SAGA-STEP-001"
verification:
  type: review
  source: "specs/saga-orchestration-l0.yaml#SAGA-STEP-001"
  pattern: "A business transaction spanning multiple services/aggregates MUST be modeled as a saga — an ORDERED sequence of local transactions, each committing in its own service (SAGA-STEP-001) — never a 2PC/XA distributed transaction across the services. When a step fails, the saga MUST execute compensating transactions that undo the changes of the PRECEDING steps in REVERSE order (SAGA-COMPENSATE-001). The coordination style MUST be declared and singular — orchestration (a central coordinator) OR choreography (event-driven), not an undocumented mix (SAGA-ORCHESTRATION-001). Every forward step and every compensation MUST be idempotent + retry-safe, keyed so a redelivery is a no-op (SAGA-IDEMPOTENT-001, composes idempotency-l0). Saga progress MUST be a recoverable persisted state machine, and the coordinator's own state change + the command/event it emits MUST be atomic via a transactional outbox (SAGA-STATE-001, composes transactional-outbox). A step that does not complete within a per-step timeout MUST trigger compensation rather than hang the saga forever (SAGA-TIMEOUT-001). Reject a cross-service operation that commits step N without a compensation for steps 1..N-1, that uses XA across services, or that has no persisted recoverable state."
upstream:
  - "https://microservices.io/patterns/data/saga.html"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/checkout/service/workflow/DecrementInventoryActivity.java"
evidence:
  - source_type: external
    citation: "Chris Richardson — Saga pattern (microservices.io, definition)"
    url: "https://microservices.io/patterns/data/saga.html"
    quote: "A saga is a sequence of local transactions."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Chris Richardson — Saga pattern (microservices.io, compensation on failure)"
    url: "https://microservices.io/patterns/data/saga.html"
    quote: "If a local transaction fails because it violates a business rule then the saga executes a series of compensating transactions that undo the changes that were made by the preceding local transactions."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) DecrementInventoryActivity — register-compensation-before-side-effect grounding (SAGA-COMPENSATE-002): the rollback state is registered via ActivityStateManagerImpl.registerState(...) BEFORE inventoryService.decrementInventory(...) runs, so a crash mid-decrement is still compensatable; the non-compensatable CompleteOrderActivity is ordered last"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/checkout/service/workflow/DecrementInventoryActivity.java"
    quote: "Register the map with the rollback state object early on; this allows the extension handlers to incrementally"
    quoted_at: "2026-06-25"
decided_at: "2026-06-06"
---

## A multi-service business transaction MUST be a saga — ordered local transactions with reverse-order compensation

**Impact: HIGH — A business operation that touches more than one service or aggregate (place an order, then reserve credit in the billing service, then reserve stock in the inventory service) cannot be wrapped in a single ACID transaction — there is no shared database to roll back. If you simply run the steps and one fails midway, the already-committed steps stay committed: credit is reserved against an order that will never ship, inventory is decremented for a payment that never cleared, and there is no mechanism to walk it back. The corruption is silent and accumulates. The saga is the answer — per Chris Richardson, *a saga is a sequence of local transactions*, and *if a local transaction fails because it violates a business rule then the saga executes a series of compensating transactions that undo the changes that were made by the preceding local transactions.***

There are six load-bearing requirements — the items of `specs/saga-orchestration-l0.yaml`, all governed by this rule (the compensation requirement has two facets: the reverse-order *firing* and the register-before-act *ordering* it depends on).

**1. Ordered local-transaction sequence (SAGA-STEP-001).** The operation is decomposed into discrete steps, each a *local* transaction committing atomically in one service. There is NO distributed transaction (2PC/XA) spanning the services — each step stands alone and is durable on its own commit.

**2. Reverse-order compensation (SAGA-COMPENSATE-001).** Every step that has an externally-visible effect has a *compensating* transaction that semantically undoes it (cancel the credit reservation, release the stock). On a failure at step N, the saga runs the compensations for steps N-1 … 1 in **reverse order** — the inverse of the forward order — so dependencies unwind cleanly. A compensation is a new transaction, not a rollback (the original already committed).

**2b. Register the compensation BEFORE the side effect (SAGA-COMPENSATE-002).** Reverse-order firing (requirement 2) can only undo a leg whose compensation was *registered*. This is a **within-step** ordering, orthogonal to the **between-step** pivot placement requirement 2 already covers ("pivot steps placed so that no committed compensatable step follows them"): even with steps ordered correctly, a single step that mutates external state and only THEN records how to undo it has a crash window that orphans the effect — stock is gone with no registered compensation, and the reverse-order rollback silently skips it. So each step MUST durably register its compensating action **before** — or atomically within the same local transaction as — performing its irreversible side effect, never after. This is the register-before-act ordering requirement 2 assumes but does not itself state — the per-step-grain analog of the transactional-outbox "write the row in the same tx as the state change" discipline (requirement 5), applied to compensation registration rather than event emission. (Broadleaf's checkout `DecrementInventoryActivity` registers its rollback state *before* the decrement — *"Register the map with the rollback state object early on … but still throw an exception"*.)

**3. One declared coordination style (SAGA-ORCHESTRATION-001).** The saga is EITHER orchestration (a central coordinator tells each service what to do and reacts to replies) OR choreography (each service emits events others react to) — declared explicitly, used consistently. An undocumented mix makes the control flow impossible to reason about or recover.

**4. Idempotent, retry-safe steps (SAGA-IDEMPOTENT-001).** Because messages are delivered at-least-once and steps are retried, every forward step AND every compensation MUST be idempotent — keyed (on the saga id + step) so a redelivery or retry is a no-op, never a double effect. Composes `idempotency-l0`.

**5. Recoverable persisted state (SAGA-STATE-001).** The saga's progress is a persisted state machine (which steps committed, which compensations ran), so a coordinator crash resumes rather than loses the saga. The coordinator's own state transition and the command/event it emits MUST commit atomically — via a transactional outbox, never a dual write. Composes `transactional-outbox`.

**6. Per-step timeout → compensation (SAGA-TIMEOUT-001).** A step that does not reply within a declared per-step timeout MUST NOT hang the saga indefinitely — the timeout triggers the failure path (compensate the prior steps), so a stuck downstream cannot strand resources reserved by earlier steps forever.

**Incorrect — multi-service operation with no compensation; a late failure leaves committed steps inconsistent:**

```java
@Transactional                                  // VIOLATION: a local @Transactional cannot span services
public void placeOrder(PlaceOrder cmd) {
    orderService.create(cmd);                    // commits in order DB
    billingService.reserveCredit(cmd.amount());  // commits in billing DB
    inventoryService.reserve(cmd.items());       // throws → order + credit already committed, nothing undoes them
}
```

**Correct — orchestrated saga: each step registers its compensation BEFORE its own side effect (keyed on the stable saga key), failure compensates in reverse, state persisted via outbox:**

```java
// Orchestrator drives an ordered sequence; each step is a local txn in its service.
// State is a persisted machine; each transition + emitted command commits atomically via the outbox.
sagaState = SagaState.start(cmd);                         // persisted (SAGA-STATE-001)
try {
    // Each compensation is registered BEFORE its step's side effect (SAGA-COMPENSATE-002),
    // keyed on the stable sagaKey — NOT on the step's not-yet-known result id — so a crash
    // mid-effect still has a durably-registered compensation. The compensation looks the
    // entity up by sagaKey, so it is a no-op if the effect never landed (SAGA-IDEMPOTENT-001).
    sagaState.registerCompensation("cancel-order", sagaKey);    // register FIRST ...
    order.create(cmd, sagaKey);                                 // ... THEN the side effect (idempotent on sagaKey)
    sagaState.registerCompensation("release-credit", sagaKey);
    billing.reserveCredit(cmd, sagaKey);
    sagaState.registerCompensation("release-stock", sagaKey);
    inventory.reserve(cmd, sagaKey);                           // within per-step timeout (SAGA-TIMEOUT-001)
    sagaState.markCompleted();                                 // pivot (confirm) ordered last (SAGA-COMPENSATE-001)
} catch (StepFailedException | StepTimeoutException e) {
    sagaState.compensateAll();    // fires registered compensations in REVERSE order (SAGA-COMPENSATE-001);
    sagaState.markCompensated();  // each is a no-op if its step never committed (SAGA-IDEMPOTENT-001)
}
```

Verification: review-tier. Saga correctness is a distributed-control-flow property with no compile-time signal — a no-compensation pipeline compiles and works on the happy path, failing only when a mid-sequence step fails in production. Verify by review against `specs/saga-orchestration-l0.yaml`: the operation is an ordered sequence of local transactions (no XA); every effecting step has a compensation and failures run them in reverse; one declared coordination style; steps and compensations are idempotent on the saga key; saga state is persisted and the coordinator emits via a transactional outbox; a per-step timeout drives compensation. When a fork-receiver wires a real IT that fails step N and asserts steps 1..N-1 are compensated, this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [Chris Richardson — Saga pattern (sequence of local transactions with compensating transactions)](https://microservices.io/patterns/data/saga.html)
