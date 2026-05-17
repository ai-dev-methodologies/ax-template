---
title: Message and event payloads must be Java records (immutable by construction)
impact: MEDIUM
impactDescription: "Mutable payloads can be modified after publish — the in-flight copy and the delivered copy disagree"
tags:
  - messaging
  - immutability
  - records
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MESSAGING-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MESSAGING-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html"
evidence:
  - upstream_id: spring-application-events
    section: "Spring Framework — event payload semantics"
    quote: "ApplicationEvent"
  - source_type: external
    citation: "JEP 395 — Records (final)"
    url: "https://openjdk.org/jeps/395"
---

## Message and event payloads must be Java records (immutable by construction)

**Impact: MEDIUM — Mutable payloads can be modified after publish — the in-flight copy and the delivered copy disagree**

A POJO with setters that the publisher pushes onto a queue can be mutated by the caller *after* `publish()` returns. Depending on the serializer's timing — and on whether the in-process bus passes by reference versus by copy — the consumer may observe the mutated state, the original state, or worse, a half-mutated state. Records make the question moot: every component is final, there are no setters, and any "change" produces a new record instance. JEP 395 (Java 16) finalized records exactly for these transport-layer value carriers.

**Incorrect — mutable POJO as payload:**

```java
public class OrderPlacedEvent {
    private String orderId;
    private String customerId;
    public void setCustomerId(String v) { this.customerId = v; }   // mutable AFTER publish
    // ...
}
```

**Correct — record payload, every component final by construction:**

```java
public record OrderPlacedEvent(String orderId, String customerId, Instant placedAt) {}

OrderPlacedEvent evt = new OrderPlacedEvent("ord-123", "cust-42", Instant.now());
publisher.publish(MessageTopics.ORDER_PLACED, evt);
// no setters, every component final, equals/hashCode/toString auto-generated
```

(Earlier iterations of this rule used `long amountCents` to illustrate a payload
field. That example is intentionally avoided here because monetary fields are
governed by `lang-bigdecimal-for-money.md`, which mandates `BigDecimal` unless
the codebase commits whole-system to integer minor-units. Using a non-monetary
field (`customerId`) keeps the immutability lesson clear without colliding with
the monetary-precision rule.)

Verification: `./gradlew testPractices --tests "*PayloadRecord*"` asserts `OrderPlacedEvent.class.isRecord()` and that every declared field is final.

Reference: [JEP 395 — Records](https://openjdk.org/jeps/395)
