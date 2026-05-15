---
title: Prefer @EntityGraph for annotation-driven fetch shape
impact: MEDIUM
impactDescription: "Same N+1 remedy as JOIN FETCH but expressed declaratively at the repository surface"
tags:
  - persistence
  - jpa
  - entity-graph
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-002
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
evidence:
  - upstream_id: spring-jpa-fetching
    section: "Spring Data JPA — EntityGraph attribute paths"
    quote: "EntityGraph"
  - source_type: external
    citation: "Spring Data JPA Reference — Configuring Fetch- and LoadGraphs"
    url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph"
---

## Prefer @EntityGraph for annotation-driven fetch shape

**Impact: MEDIUM — Same N+1 remedy as JOIN FETCH but expressed declaratively at the repository surface**

`JOIN FETCH` works, but it tangles JPQL projection with fetch shape and forces every variant query to repeat the join. `@EntityGraph(attributePaths = {"children"})` keeps the JPQL focused on filtering and declares the fetch contract at the method signature. Spring Data merges the graph hint into the query when it's executed — same single round-trip, cleaner separation.

**Incorrect — fetch shape woven into every JPQL string:**

```java
@Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children WHERE p.tenantId = :tenantId")
List<Parent> findByTenantWithChildren(Long tenantId);

@Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children WHERE p.archived = false")
List<Parent> findActiveWithChildren();
```

**Correct — single fetch contract at the annotation:**

```java
@EntityGraph(attributePaths = {"children"})
@Query("SELECT p FROM Parent p WHERE p.tenantId = :tenantId")
List<Parent> findByTenant(Long tenantId);

@EntityGraph(attributePaths = {"children"})
@Query("SELECT p FROM Parent p WHERE p.archived = false")
List<Parent> findActive();
```

Verification: `./gradlew testPractices --tests "*EntityGraph*"` asserts the annotation-driven method produces exactly one prepared statement on the seeded 3×2 fixture.

Reference: [Spring Data JPA — EntityGraph](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph)
