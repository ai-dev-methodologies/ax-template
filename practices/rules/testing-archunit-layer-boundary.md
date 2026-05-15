---
title: Enforce controller → service → repository layering with ArchUnit
impact: MEDIUM
impactDescription: "Mechanical check prevents upward layer references that compile-time can't catch"
tags:
  - testing
  - archunit
  - architecture
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-002
upstream:
  - "https://www.archunit.org/userguide/html/000_Index.html"
evidence:
  - upstream_id: archunit-userguide
    section: "ArchUnit User Guide — Rules"
    quote: "ArchRule"
  - source_type: external
    citation: "ArchUnit User Guide — Rules and Slicing"
    url: "https://www.archunit.org/userguide/html/000_Index.html"
---

## Enforce controller → service → repository layering with ArchUnit

**Impact: MEDIUM — Mechanical check prevents upward layer references that compile-time can't catch**

Java's compiler is happy to let a `*Service` import a `*Controller` — the bytecode is perfectly valid. The architecture-level rule "services don't depend on controllers" lives only in human review and slowly erodes. ArchUnit moves that rule into the test suite: a single `noClasses()...should().dependOnClassesThat()` rule scans the bytecode and fails the build when a *Service or *Repository reaches upward into the layer above it.

**Incorrect — a service that imports a controller (no compile error, slow architectural rot):**

```java
@Service
public class ReportService {
    private final ReportController controller;        // upward dependency
    public Result generate() {
        return controller.handle(...);                 // service calling controller is upside-down
    }
}
```

**Correct — ArchUnit rule fails the build on the violation:**

```java
@Test
void servicesDoNotDependOnControllers() {
    JavaClasses classes = new ClassFileImporter()
            .importPackages("com.example.app");
    noClasses().that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
            .check(classes);
}
```

Verification: `./gradlew testPractices --tests "*ArchitectureLayerBoundary*"` runs the layer rule for the practices/ subtree and asserts no *Service → *Controller and no *Repository → *(Controller|Service) edges exist.

Reference: [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)
