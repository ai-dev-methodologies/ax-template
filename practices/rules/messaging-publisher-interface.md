---
title: Service-layer publishers must depend on an abstract MessagePublisher interface
impact: HIGH
impactDescription: "A concrete-typed publisher field couples the domain to one broker SDK — broker swap becomes a refactor"
tags:
  - messaging
  - abstraction
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MESSAGING-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MESSAGING-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html"
evidence:
  - upstream_id: spring-application-events
    section: "Spring Framework — ApplicationEventPublisher abstraction"
    quote: "ApplicationEventPublisher"
  - source_type: external
    citation: "Spring Framework Reference — Standard and Custom Events"
    url: "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events"
---

**Current ax-template adapter (2026-05-16):** `SpringEventMessagePublisher` (@Primary) — uses Spring's built-in `ApplicationEventPublisher` for in-process publish-subscribe, zero broker dependency. `InMemoryMessagePublisher` is the test-only impl (not @Component). Kafka/RabbitMQ adapters plug in behind the same interface when broker infrastructure is decided.

## Service-layer publishers must depend on an abstract MessagePublisher interface

**Impact: HIGH — A concrete-typed publisher field couples the domain to one broker SDK — broker swap becomes a refactor**

A service that holds a `KafkaTemplate<String, OrderPlacedEvent>` field has *imported the broker* into the domain layer — the broker's serialization model, retry semantics, and partitioning concept are now domain concepts. Swapping Kafka for RabbitMQ or going broker-less for tests means rewriting every service that publishes. The remedy is the standard hexagonal pattern: the domain owns an abstract `MessagePublisher` interface; concrete impls (`KafkaMessagePublisher`, `RabbitMessagePublisher`, `InMemoryMessagePublisher` for tests) live in an adapter package and are wired via Spring. The current template ships `SpringEventMessagePublisher` (@Primary, backed by Spring's ApplicationEventPublisher) as the real adapter; `InMemoryMessagePublisher` is test-only (not a @Component). Broker adapters plug in later behind the same interface.

**Incorrect — service couples to the broker SDK:**

```java
@Service
public class OrderEventPublisher {
    private final KafkaTemplate<String, OrderPlacedEvent> kafka;   // broker SDK leaks into domain
    public OrderEventPublisher(KafkaTemplate<String, OrderPlacedEvent> kafka) { this.kafka = kafka; }
    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafka.send("order.placed", event.orderId(), event);
    }
}
```

**Correct — service depends on the domain-owned interface:**

```java
public interface MessagePublisher {
    void publish(String topic, Object payload);
}

@Service
public class OrderEventPublisher {
    private final MessagePublisher publisher;                       // interface, not implementation
    public OrderEventPublisher(MessagePublisher publisher) { this.publisher = publisher; }
    public void publishOrderPlaced(OrderPlacedEvent event) {
        publisher.publish(MessageTopics.ORDER_PLACED, event);
    }
}
```

Verification: `./gradlew testPractices --tests "*PublisherInterface*"` reflects on `OrderEventPublisher.publisher` and asserts the declared type equals `MessagePublisher.class` and `isInterface()`.

Reference: [Spring Framework — Standard and Custom Events (ApplicationEventPublisher abstraction)](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
