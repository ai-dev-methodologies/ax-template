---
title: State machine transitions must be atomic — @Version + transactional boundary + explicit transition method
impact: HIGH
impactDescription: "Concurrent transitions on the same workflow entity must produce exactly one winner; the loser gets a 409, not a corrupted state"
tags:
  - persistence
  - jpa
  - state-machine
  - concurrency
spec_ref: "specs/payment-l0.yaml#PAYMENT-STATE-002"
verification:
  gradle_task: testPayment
  tag: PAYMENT-STATE-002
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html"
  - "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic"
evidence:
  - upstream_id: spring-tx-declarative
    section: "Spring Framework — declarative transaction management"
    quote: "transaction"
  - source_type: external
    citation: "Hibernate User Guide — Optimistic Locking (@Version)"
    url: "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic"
  - source_type: external
    citation: "Spring Data JPA — Locking"
    url: "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html"
---

## State machine transitions must be atomic — @Version + transactional boundary + explicit transition method

**Impact: HIGH — Concurrent transitions on the same workflow entity must produce exactly one winner; the loser gets a 409, not a corrupted state**

Any entity with a lifecycle — `WorkItem` (QUEUED → RUNNING → DONE → FAILED), `Order` (PENDING → CONFIRMED → SHIPPED → DELIVERED), `Subscription` (TRIAL → ACTIVE → PAUSED → CANCELLED), `Payment` (CREATED → AUTHORIZED → CAPTURED → REFUNDED) — encodes a transition graph. Three things must hold simultaneously, and the bug surface for missing any of them is identical: silent corruption of the entity's state under concurrency. (1) The legal transitions must live in a single dedicated method (or a `StateMachine` companion type) that throws on illegal events — no direct field mutation of the state column anywhere else in the codebase. (2) Each transition must execute inside a transactional boundary, so the state column write and any dependent writes (audit ledger, denormalized counters, outgoing event publish) commit atomically or roll back together. (3) The entity must carry `@Version` so that two concurrent transactions racing the same entity collide on optimistic-lock check — one wins, the other surfaces as `ObjectOptimisticLockingFailureException` which the exception handler translates to HTTP 409. `persistence-optimistic-locking.md` covers the @Version primitive in isolation; this rule combines it with the dedicated transition method and the transactional boundary, which is the shape required for any workflow state machine.

**Incorrect — direct field mutation, no @Version, no transition method:**

```java
@Entity
public class WorkItem {
    @Id @GeneratedValue Long id;
    @Enumerated(EnumType.STRING) WorkState state;
    // no @Version — two concurrent transitions both succeed, last writer wins
}

@Service
public class WorkService {
    @Transactional
    public void markRunning(long id) {
        WorkItem item = repo.findById(id).orElseThrow();
        item.setState(WorkState.RUNNING);          // direct mutation, no transition check
        repo.save(item);
    }

    @Transactional
    public void markDone(long id) {
        WorkItem item = repo.findById(id).orElseThrow();
        item.setState(WorkState.DONE);             // can be called from QUEUED — skips RUNNING
        repo.save(item);
    }
}
```

**Correct — dedicated transition method on the entity + @Version + transactional caller:**

<!-- catalog-example-ok: WorkItem WorkState WorkEvent WorkStateMachine — intentionally generic; the reference impl is PaymentStateMachine -->
```java
@Entity
public class WorkItem {
    @Id @GeneratedValue Long id;

    @Enumerated(EnumType.STRING)
    private WorkState state;

    @Version
    private long version;             // optimistic lock — bumped on every persist

    public void transition(WorkEvent event) {
        WorkState next = WorkStateMachine.transition(this.state, event);
        // WorkStateMachine.transition throws IllegalStateTransitionException on an
        // illegal (from, event) pair — never returns null.
        this.state = next;            // single mutation site, gated by the state machine
    }

    public WorkState state() { return state; }
}

@Service
public class WorkService {
    @Transactional
    public void apply(long id, WorkEvent event) {
        WorkItem item = repo.findById(id).orElseThrow();
        item.transition(event);       // throws on illegal event
        repo.save(item);              // @Version mismatch → ObjectOptimisticLockingFailureException → 409
    }
}
```

The `WorkStateMachine.transition(state, event)` pure function returns the next state or **throws** `IllegalStateTransitionException` on an illegal transition — it never returns `null`. The exception handler maps `IllegalStateTransitionException` to HTTP 409 with an RFC 7807 `application/problem+json` body that includes `currentState` and `attemptedEvent` extensions, so clients can react programmatically.

Verification: `./gradlew testPayment --tests "*StateMachine*"` exercises the legal-transition matrix (all defined transitions succeed; all undefined transitions throw `IllegalStateTransitionException`) and an optimistic-lock check: JPA `@Version` is asserted to reject a stale-version re-transition (the test approximates the race sequentially; the full two-thread CAPTURE race is deferred to the US-011 concurrency suite). The optimistic-lock loser surfaces as a 409.

Reference: [Spring Data JPA — Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)

Reference: [Hibernate User Guide — Optimistic Locking](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)
