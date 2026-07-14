---
title: Prevent N+1 queries with explicit fetch shape
impact: HIGH
impactDescription: "Reduces parent-collection iteration from N+1 SELECTs to 1"
tags:
  - persistence
  - jpa
  - n-plus-one
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-001
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/fetching.html"
evidence:
  - upstream_id: spring-jpa-fetching
    section: EntityGraph definition + fetch plan
    quote: "the @EntityGraph annotation, which lets you reference a @NamedEntityGraph definition. You can use that annotation on an entity to configure the fetch plan of the resulting query."
  - source_type: external
    citation: 'Spring Data JPA Reference — Fetching strategies (JOIN FETCH and @EntityGraph)'
    url: 'https://docs.spring.io/spring-data/jpa/reference/jpa/fetching.html'
  - source_type: external
    citation: 'Hibernate User Guide — Performance §Fetching'
    url: 'https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#fetching'
---

## Prevent N+1 queries with explicit fetch shape

**Impact: HIGH — Reduces parent-collection iteration from N+1 SELECTs to 1**

Lazy-loaded associations issue a SELECT each time they are touched. Iterating a parent collection and reading its children naively causes 1 + N queries — invisible in development with a few rows, catastrophic in production. The remedy is to declare the fetch shape at the query layer using `JOIN FETCH` or `@EntityGraph`, so the database returns the full graph in a single round trip.

**Incorrect — implicit lazy iteration triggers N+1:**

```java
var parents = parentRepo.findAll();
parents.forEach(p -> p.getChildren().size()); // each access fires a SELECT
```

**Correct — fetch shape declared at the query:**

```java
public interface ParentRepository extends JpaRepository<Parent, Long> {
    @Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children")
    List<Parent> findAllWithChildren();
}
```

Verification: `./gradlew testPractices --tests "*NPlusOne*"` exercises both paths and asserts `Statistics.getPrepareStatementCount()` equals 1 for the fetched path.

Reference: [Spring Data JPA — Fetching strategies](https://docs.spring.io/spring-data/jpa/reference/jpa/fetching.html)
