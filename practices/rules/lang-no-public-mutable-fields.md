---
title: No public, non-static, non-final instance fields outside records
impact: MEDIUM
impactDescription: "Public mutable fields bypass encapsulation and break every dependent on next refactor"
tags:
  - lang
  - encapsulation
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-LANG-003
upstream:
  - "https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html"
evidence:
  - source_type: external
    citation: "Java Language Specification §8.3.1 — Field Modifiers"
    url: "https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.1"
  - source_type: external
    citation: "Effective Java (Bloch, 3rd ed.) — Item 16: In public classes, use accessor methods, not public fields"
    url: "https://www.oreilly.com/library/view/effective-java/9780134686097/"
---

## No public, non-static, non-final instance fields outside records

**Impact: MEDIUM — Public mutable fields bypass encapsulation and break every dependent on next refactor**

`public String name;` looks convenient. It also means the field cannot be renamed, retyped, validated on set, or replaced with a derived accessor without breaking every caller — and there is no observable point where invariants can be enforced. Effective Java Item 16 codifies the rule: "in public classes, use accessor methods, not public fields". Constants (`public static final`) are unaffected. Record components project public accessor *methods*, not fields, so records are exempt by construction.

**Incorrect — public mutable instance field:**

```java
public class Counter {
    public int value;                      // anything can set; no validation; no future-proofing
}
```

**Correct — record or accessor method on the class:**

```java
public record CounterSnapshot(int value) {}

// or, if you need behavior on a class:
public class Counter {
    private int value;                     // encapsulated
    public int value() { return value; }
    public void increment() { value++; }
}
```

Verification: `./gradlew testPractices --tests "*NoPublicMutableFields*"` runs an ArchUnit field rule that picks every public, non-static, non-final field on a non-record class and fails if any are found.

Reference: [JLS §8.3.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.1) · Effective Java Item 16
