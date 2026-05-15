---
title: Trivially-true rule (adversarial case — fake good rule placeholder)
impact: LOW
impactDescription: "Adversarial fixture for P2-A1 — would pass spec_ref_guard but say nothing"
tags:
  - adversarial
  - placeholder
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-001
upstream:
  - "https://example.com/placeholder"
---

## Trivially-true rule

This rule is intentionally vacuous: it references a real spec item so `spec_ref_guard.sh`
passes, but the body offers no testable substance. Its purpose is to drive the design of
the **rule-substance-guard** (P2-A6) — a future guard that scores rule body
substance (incorrect/correct examples present, references non-empty, prose length, etc.)
and rejects clearly empty rules.

For now this case is a placeholder. `spec_ref_guard` cannot catch it; that is the gap
P2-A6 must close.

**Incorrect:**

```java
// no real example
```

**Correct:**

```java
// no real example
```

Reference: [placeholder](https://example.com/placeholder)
