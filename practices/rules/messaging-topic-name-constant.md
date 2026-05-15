---
title: Topic / routing-key names must be public-static-final constants, not inline string literals
impact: MEDIUM
impactDescription: "Inline topic literals diverge silently between publisher and consumer; the queue goes empty in prod"
tags:
  - messaging
  - constants
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MESSAGING-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MESSAGING-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html"
evidence:
  - upstream_id: spring-application-events
    section: "Spring Framework — event identity / topic identity"
    quote: "event"
  - source_type: external
    citation: "Effective Java (3rd ed.) — Item 22: Use interfaces only to define types (constants-class pattern)"
    url: "https://www.oreilly.com/library/view/effective-java-third/9780134686097/"
---

## Topic / routing-key names must be public-static-final constants, not inline string literals

**Impact: MEDIUM — Inline topic literals diverge silently between publisher and consumer; the queue goes empty in prod**

When a topic name is an inline `"order.placed"` in the publisher and an inline `"orders.placed"` in the consumer (one extra `s`), the build is green, the tests pass, and the queue silently goes empty in production. The compiler doesn't know two string literals were meant to refer to the same topic. A single `MessageTopics` final class with `public static final String ORDER_PLACED = "practices.order.placed"` forces both ends through the same symbol — a rename is one edit, not a code-search-replace across the codebase. The constants holder is a `final` class with a private constructor so it cannot be subclassed or instantiated (Effective Java Item 22 — interfaces define types, not constants holders).

**Incorrect — inline literals at every call site:**

```java
publisher.publish("order.placed", event);                          // publisher
// ... elsewhere ...
@KafkaListener(topics = "orders.placed")                           // consumer — one extra 's', silent break
public void onOrderPlaced(OrderPlacedEvent event) { ... }
```

**Correct — single constants class, both sides reference it:**

```java
public final class MessageTopics {
    public static final String ORDER_PLACED = "practices.order.placed";
    private MessageTopics() {}                                     // not instantiable
}

publisher.publish(MessageTopics.ORDER_PLACED, event);              // publisher
@KafkaListener(topics = MessageTopics.ORDER_PLACED)                // consumer — same symbol, rename-safe
public void onOrderPlaced(OrderPlacedEvent event) { ... }
```

Verification: `./gradlew testPractices --tests "*TopicNameConstant*"` reflects on `MessageTopics.ORDER_PLACED` and asserts public + static + final + String, plus the holder class itself is final.

Reference: [Spring Framework — Standard and Custom Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events) · Effective Java (3rd ed.) — Item 22: Use interfaces only to define types.
