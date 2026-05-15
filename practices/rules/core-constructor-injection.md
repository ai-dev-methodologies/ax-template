---
title: Use constructor injection with final fields
impact: HIGH
impactDescription: "Surfaces missing/circular dependencies at construction time; enables immutability + plain-JUnit tests"
tags:
  - core
  - di
  - immutability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CORE-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CORE-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection"
evidence:
  - upstream_id: spring-beans-constructor-injection
    section: Constructor-based Dependency Injection
    quote: 'dency-injected with constructor injection: Java Kotlin public class SimpleMovieLister { // the SimpleMovieLister has a dependency on a MovieFinder private final MovieFinder movieFinder; // a constructor so that the Spring container can inject a MovieFinder pub'
  - source_type: external
    citation: 'Spring Framework Reference — §Constructor-based Dependency Injection'
    url: 'https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection'
  - source_type: external
    citation: 'Spring Team Blog — Why we changed @Autowired on constructors recommendation (Sep 2016)'
    url: 'https://spring.io/blog/2016/03/04/core-container-refinements-in-spring-framework-4-3'
---

## Use constructor injection with final fields

**Impact: HIGH — Surfaces missing/circular dependencies at construction time; enables immutability + plain-JUnit tests**

Field injection puts an `@Autowired` annotation on a non-final field, leaving the dependency mutable, mockable only via reflection, and silent about circular dependencies until the application starts. Constructor injection declares the same dependency as a `final` field initialized in the constructor — the class cannot be instantiated without it, the dependency cannot be reassigned, and unit tests instantiate the bean with plain `new`.

**Incorrect — field injection:**

```java
@Service
public class FieldInjectedService {
    @Autowired                       // requires reflection in tests; field is non-final
    private ParentRepository parents;

    public long countParents() {
        return parents.count();
    }
}
```

**Correct — constructor injection with `final`:**

```java
@Service
public class ConstructorInjectedService {
    private final ParentRepository parents;

    public ConstructorInjectedService(ParentRepository parents) {
        this.parents = parents;
    }

    public long countParents() {
        return parents.count();
    }
}
```

Verification: `./gradlew testPractices --tests "*ConstructorInjection*"` asserts the correct fixture has a `final` field and a matching single-arg constructor; the anti-pattern fixture has a non-final field.

Reference: [Spring Framework — Constructor-based DI](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection)
