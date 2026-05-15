---
title: Emit structured key-value pairs, not concatenated log strings
impact: MEDIUM
impactDescription: "JSON appenders can index typed fields; concatenated strings can only be grep-searched"
tags:
  - observability
  - logging
  - structured-logs
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-OBS-001
upstream:
  - "https://logback.qos.ch/manual/layouts.html"
evidence:
  - upstream_id: logback-layouts
    section: "Logback PatternLayout and structured encoders"
    quote: "PatternLayout"
  - source_type: external
    citation: "SLF4J 2.x — Fluent logging API (addKeyValue)"
    url: "https://www.slf4j.org/manual.html#fluent"
---

## Emit structured key-value pairs, not concatenated log strings

**Impact: MEDIUM — JSON appenders can index typed fields; concatenated strings can only be grep-searched**

Once logs land in a search system (Elasticsearch, Loki, CloudWatch Insights), the difference between `"order processed order_id=ord-123 amount=42"` and a structured event `{message: "order processed", order_id: "ord-123", amount: 42}` is the difference between regex-hunting and `order_id:"ord-123"` queries. SLF4J 2.x's fluent API and Logback's structured encoders preserve typed key-value pairs end-to-end; concatenation discards them at the call site.

**Incorrect — string-concatenated log:**

```java
String message = "order processed order_id=" + order.id() + " amount=" + order.amount();
log.info(message);
```

**Correct — structured key-value pairs:**

```java
log.atInfo()
   .addKeyValue("order_id", order.id())
   .addKeyValue("amount", order.amount())
   .setMessage("order processed")
   .log();
```

Verification: `./gradlew testPractices --tests "*StructuredLogging*"` attaches a Logback ListAppender, exercises both code paths, and asserts the structured path emits `KeyValuePair`s while the concatenated path emits none.

Reference: [SLF4J Fluent API](https://www.slf4j.org/manual.html#fluent) · [Logback Layouts](https://logback.qos.ch/manual/layouts.html)
