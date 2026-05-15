---
title: Optional is a return type — never a field, never a parameter
impact: MEDIUM
impactDescription: "Optional as a field adds allocation, defeats serialization, and is rarely meaningful"
tags:
  - quality
  - optional
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-QUALITY-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-QUALITY-001
upstream:
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html"
evidence:
  - source_type: external
    citation: "Effective Java (Bloch, 3rd ed.) — Item 55: Return optionals judiciously"
    url: "https://www.oreilly.com/library/view/effective-java/9780134686097/"
  - source_type: external
    citation: "Stuart Marks (Oracle) — Optional class Javadoc API note"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html"
---

## Optional is a return type — never a field, never a parameter

**Impact: MEDIUM — Optional as a field adds allocation, defeats serialization, and is rarely meaningful**

`Optional<T>` was designed to communicate "may be absent" at the API boundary — the return type of a finder, an aggregate, a parser. As a field it produces an extra wrapper allocation per instance, defeats serialization (it is not `Serializable`), and is rarely more expressive than a nullable field. As a parameter it forces the caller to wrap a value it already has — the caller cannot pass `null`, but it cannot pass the value either. Effective Java Item 55 codifies the restriction: return type only.

**Incorrect — Optional as a field:**

```java
public class Order {
    private Optional<Discount> discount;          // extra allocation per Order; serialization broken
    public Optional<Discount> getDiscount() { return discount; }
}
```

**Correct — nullable field, Optional only at the boundary:**

```java
public class Order {
    private Discount discount;                    // may be null
    public Optional<Discount> getDiscount() {
        return Optional.ofNullable(discount);     // Optional appears at the return boundary only
    }
}
```

Verification: `./gradlew testPractices --tests "*OptionalNotAsField*"` runs an ArchUnit rule that rejects any field with raw type `Optional` in the practices/ subtree.

Reference: Effective Java Item 55 · [Optional Javadoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html)
