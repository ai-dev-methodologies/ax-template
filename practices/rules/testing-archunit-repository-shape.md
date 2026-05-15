---
title: Classes named *Repository must extend Spring Data's JpaRepository
impact: MEDIUM
impactDescription: "Stops hand-rolled \"repository\" services that bypass the data-access layer's guarantees"
tags:
  - testing
  - archunit
  - persistence
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-004"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-004
upstream:
  - "https://www.archunit.org/userguide/html/000_Index.html"
evidence:
  - upstream_id: archunit-userguide
    section: "ArchUnit User Guide — class predicates"
    quote: "JavaClasses"
  - source_type: external
    citation: "Spring Data JPA — Defining repository interfaces"
    url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
---

## Classes named *Repository must extend Spring Data's JpaRepository

**Impact: MEDIUM — Stops hand-rolled "repository" services that bypass the data-access layer's guarantees**

`@Service public class OrderRepository { ... }` is a common drift pattern: a service class that calls `entityManager.createQuery(...)` directly, named "Repository" because that is where the data-access code lives in the developer's head. The drift defeats Spring Data's query derivation, ignores its method-level transaction defaults, sidesteps the `@Repository` exception translation, and confuses every reader who expects the name `*Repository` to mean a Spring Data interface. The ArchUnit rule enforces the shape: classes named *Repository must be interfaces that extend `JpaRepository`.

**Incorrect — hand-rolled "repository" as a class:**

```java
@Service                                          // not @Repository, not Spring Data
public class OrderRepository {                    // class, not interface
    private final EntityManager em;
    public Order findById(Long id) {
        return em.createQuery(...).getSingleResult();
    }
}
```

**Correct — Spring Data interface:**

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByCustomerEmail(String email);
}
```

Verification: `./gradlew testPractices --tests "*RepositoriesExtendJpa*"` runs an ArchUnit rule that picks up every `*Repository` class and asserts it is an interface assignable to `JpaRepository`.

Reference: [Spring Data JPA — Defining Repository Interfaces](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
