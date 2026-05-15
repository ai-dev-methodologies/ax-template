---
title: Utility classes must be final + private no-arg constructor
impact: LOW
impactDescription: "Without it the class is subclassable (instance state creeps in) and instantiable (silent no-op)"
tags:
  - quality
  - utility-class
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-QUALITY-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-QUALITY-002
upstream:
  - "https://www.oreilly.com/library/view/effective-java/9780134686097/"
evidence:
  - source_type: external
    citation: "Effective Java (Bloch, 3rd ed.) — Item 4: Enforce noninstantiability with a private constructor"
    url: "https://www.oreilly.com/library/view/effective-java/9780134686097/"
  - source_type: external
    citation: "JLS §8.8.10 — Preventing Instantiation of a Class"
    url: "https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html"
---

## Utility classes must be final + private no-arg constructor

**Impact: LOW — Without it the class is subclassable (instance state creeps in) and instantiable (silent no-op)**

A "utility class" — one whose every member is `static` — exists only as a namespace for related functions. Without explicit constraints the JVM gives it a default public constructor and lets anyone subclass it. Both are silent: a `new PiiRedactor()` is a no-op that suggests the class has state, and `class HardenedRedactor extends PiiRedactor { String key = ...; }` adds the state the original class deliberately omitted. Effective Java Item 4: declare the class `final` and give it a single private no-arg constructor.

**Incorrect — implicit public constructor + subclassable:**

```java
public class PiiRedactor {                       // not final, no constructor declared
    public static String redact(String s) { ... }
    // implicit `public PiiRedactor() {}` — anyone can instantiate or subclass
}
```

**Correct — final class with private constructor:**

```java
public final class PiiRedactor {
    private PiiRedactor() { /* utility */ }      // explicitly uninstantiable
    public static String redact(String s) { ... }
}
```

Verification: `./gradlew testPractices --tests "*UtilityClassShape*"` reflects on `PiiRedactor` and asserts the class is final, has exactly one declared constructor, that constructor takes no arguments, and is private.

Reference: Effective Java Item 4 · [JLS §8.8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html)
