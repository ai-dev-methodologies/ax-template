---
title: Forbid cyclic package dependencies with ArchUnit slicing
impact: MEDIUM
impactDescription: "Cycles block isolated testing of any module in the cycle"
tags:
  - testing
  - archunit
  - architecture
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-003
upstream:
  - "https://www.archunit.org/userguide/html/000_Index.html"
evidence:
  - upstream_id: archunit-userguide
    section: "ArchUnit User Guide — slicing rules"
    quote: "ArchRule"
  - source_type: external
    citation: "ArchUnit User Guide — Slices"
    url: "https://www.archunit.org/userguide/html/000_Index.html#_slices"
---

## Forbid cyclic package dependencies with ArchUnit slicing

**Impact: MEDIUM — Cycles block isolated testing of any module in the cycle**

If package `a` imports something from `b` and `b` imports something from `a`, neither package can be loaded, compiled (in isolation), or tested without the other — they have collapsed into one module wearing two names. Cycles also block module extraction (Spring Modulith, separate Maven module) and obscure the dependency graph for readers. ArchUnit's slicing detector partitions the namespace and reports any cycle.

**Incorrect — two packages with cross-imports:**

```java
// package com.example.users
package com.example.users;
import com.example.billing.InvoiceFormatter;       // depends on billing
...

// package com.example.billing
package com.example.billing;
import com.example.users.UserPreferences;           // depends on users — CYCLE
```

**Correct — ArchUnit rule catches the cycle:**

```java
@Test
void noCyclicPackageDependencies() {
    JavaClasses classes = new ClassFileImporter()
            .importPackages("com.example..");
    SlicesRuleDefinition.slices()
            .matching("com.example.(*)..")
            .should().beFreeOfCycles()
            .check(classes);
}
```

Verification: `./gradlew testPractices --tests "*NoCyclicPackage*"` partitions the practices/ subtree by direct child package and asserts the slice graph is acyclic.

Reference: [ArchUnit User Guide — Slices](https://www.archunit.org/userguide/html/000_Index.html#_slices)
