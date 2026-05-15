---
title: Model closed result hierarchies with sealed interface + record permits
impact: MEDIUM
impactDescription: "Compiler-enforced exhaustive handling of every terminal outcome"
tags:
  - lang
  - sealed
  - pattern-matching
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-LANG-002
upstream:
  - "https://openjdk.org/jeps/409"
evidence:
  - upstream_id: jep-409-sealed-classes
    section: "JEP 409 — Sealed Classes (Final)"
    quote: "sealed"
  - source_type: external
    citation: "JEP 409 — Sealed Classes (Final, Java 17)"
    url: "https://openjdk.org/jeps/409"
---

## Model closed result hierarchies with sealed interface + record permits

**Impact: MEDIUM — Compiler-enforced exhaustive handling of every terminal outcome**

Before sealed types, a "result" type was an `enum` (no carried data), a class hierarchy with `instanceof` chains (forgettable), or a single `Result` class with optional fields (silent wrong-state bugs). `sealed interface` + permitted records gives a closed hierarchy: each outcome carries its own typed data, and a pattern-matching `switch` over the sealed type is exhaustive — the compiler refuses to forget a branch. Adding a third outcome is a single compile-time signal: every `switch` on the type becomes a compile error until the new case is handled.

**Incorrect — boolean + nullable error field:**

```java
public class PaymentResult {
    private final boolean success;
    private final String errorCode;          // null when success — silent footgun
    private final String transactionId;      // null when failure — same
    // ...
}
```

**Correct — sealed interface + record subtypes:**

```java
public sealed interface PaymentResult permits PaymentSuccess, PaymentFailure {
    record PaymentSuccess(String txId, long amount) implements PaymentResult {}
    record PaymentFailure(String errorCode, String message) implements PaymentResult {}
}

// Exhaustive switch — compiler errors if a branch is added but not handled.
String describe(PaymentResult r) {
    return switch (r) {
        case PaymentSuccess s -> "ok:" + s.txId();
        case PaymentFailure f -> "fail:" + f.errorCode();
    };
}
```

Verification: `./gradlew testPractices --tests "*SealedResultHierarchy*"` asserts `PaymentResult.class.isSealed()`, that all `getPermittedSubclasses()` are records, and that an exhaustive `switch` over the type compiles.

Reference: [JEP 409 — Sealed Classes (Final)](https://openjdk.org/jeps/409)
