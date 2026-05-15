---
title: Rule Title Here
impact: MEDIUM
impactDescription: "Brief impact description (e.g., '20-50% improvement in X')"
tags:
  - category-prefix
  - subcategory
spec_ref: "specs/spring-practices-l0.yaml#ITEM-ID"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ITEM-ID
upstream:
  - "https://docs.spring.io/spring-framework/reference/"
# evidence: REQUIRED — at least one entry. Each entry is one of two shapes:
#   1) Anchored to a snapshot recorded in practices/upstream/_MANIFEST.yaml:
#      - upstream_id: spring-boot-3.5
#        section: "Constructor Injection"
#        quote: "Constructor injection is the recommended approach to dependency injection..."
#   2) Anchored to an external citation (RFC, JEP, vendor doc, peer-reviewed paper):
#      - source_type: external
#        citation: "Spring Framework Reference §1.4.1"
#        url: "https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection"
# A rule without evidence is rejected by practices/evals/evidence_guard.sh.
evidence:
  - source_type: external
    citation: "(replace with the standard / docs you actually consulted)"
    url: "https://docs.spring.io/spring-framework/reference/"
---

## Rule Title Here

**Impact: MEDIUM — Brief impact description**

Explain why this rule matters in 2-3 sentences. Focus on the concrete harm caused by the incorrect pattern (performance degradation, data integrity risk, test brittleness, etc.) and link it to the spec item referenced above.

**Incorrect — description of what's wrong:**

```java
// Example of the anti-pattern
// Annotate with what goes wrong and why
public List<Item> getAll() {
    return repo.findAll().stream()
        .map(e -> e.getChildren())   // N+1: lazy load inside stream
        .collect(toList());
}
```

**Correct — description of what's right:**

```java
// Fixed pattern with explanation
@Query("SELECT e FROM Entity e JOIN FETCH e.children")
public List<Item> getAll() {
    return repo.findAllWithChildren();  // single query, no N+1
}
```

Reference: [Spring Data JPA — Fetching Strategies](https://docs.spring.io/spring-data/jpa/reference/)
