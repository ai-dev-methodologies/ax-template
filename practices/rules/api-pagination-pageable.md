---
title: List endpoints must use Pageable and clamp size
impact: HIGH
impactDescription: "Unbounded list endpoints are a recurring latency + memory hazard"
tags:
  - api
  - pagination
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-API-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-API-001
upstream:
  - "https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
evidence:
  - upstream_id: spring-data-paging
    section: "Spring Data — Pageable / Page<T>"
    quote: "Pageable"
  - source_type: external
    citation: "Spring Data Commons Reference — Core Concepts (Pageable)"
    url: "https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
---

## List endpoints must use Pageable and clamp size

**Impact: HIGH — Unbounded list endpoints are a recurring latency + memory hazard**

`@GetMapping("/parents") List<Parent> all()` works fine on a developer laptop with 12 rows. The same endpoint against a production table with 4 million rows times out the connection pool, exhausts heap, or returns megabytes of JSON to a mobile client that wanted the first ten. The contract every list endpoint must enforce: accept `page` + `size` (or a `Pageable`), clamp `size` to a documented maximum, and return `Page<DTO>` so the client gets total counts and navigation links alongside the slice.

**Incorrect — unbounded findAll() exposed as a list:**

```java
@GetMapping("/parents")
public List<Parent> all() {
    return parentRepo.findAll();             // 4M rows? sure, here you go
}
```

**Correct — Pageable parameter, clamped size, Page<DTO> response:**

```java
private static final int MAX_PAGE_SIZE = 100;

@GetMapping("/v1/parents")
public Page<ParentResponse> listParents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
) {
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
    return parentRepo.findAll(pageable).map(ParentResponse::from);
}
```

Verification: `./gradlew testPractices --tests "*PaginationPageable*"` asserts the endpoint honors `?page=0&size=5`, defaults to a reasonable size, and clamps an oversize `?size=10000` to the documented maximum.

Reference: [Spring Data — Core Concepts (Pageable)](https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html)
