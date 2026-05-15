---
title: Return DTO records from controllers, never JPA entities
impact: HIGH
impactDescription: "Returning entities leaks association graphs, lazy fields, and breaks the API contract on every entity edit"
tags:
  - api
  - dto
  - persistence
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-API-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-API-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
evidence:
  - upstream_id: spring-mvc-modelattribute
    section: "Spring MVC — controller method arguments and return values"
    quote: "@ModelAttribute"
  - source_type: external
    citation: "Spring Framework Reference — Controllers and DTOs"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
---

## Return DTO records from controllers, never JPA entities

**Impact: HIGH — Returning entities leaks association graphs, lazy fields, and breaks the API contract on every entity edit**

Returning a JPA entity from a controller writes the full entity surface into the response body. Lazy associations get triggered, internal-only fields appear, and every entity refactor silently rewrites the public API contract — the day someone adds an `@OneToMany` for an internal cache, the API breaks. The remedy is a record DTO that explicitly lists which fields cross the boundary. The mapping function (`from(entity)`) is the single place the contract is defined.

**Incorrect — return the JPA entity directly:**

```java
@GetMapping("/parents")
public List<Parent> list() {
    return parents.findAll();             // children collection leaks, lazy fields trigger
}
```

**Correct — DTO record collapses the entity into a contract surface:**

```java
public record ParentResponse(Long id, String name, int childCount) {
    public static ParentResponse from(Parent p) {
        return new ParentResponse(p.getId(), p.getName(), p.getChildren().size());
    }
}

@GetMapping("/v1/parents")
public Page<ParentResponse> list(Pageable pageable) {
    return parents.findAll(pageable).map(ParentResponse::from);
}
```

Verification: `./gradlew testPractices --tests "*NoEntityLeak*"` asserts the JSON body contains the DTO field (`childCount`) and does NOT contain the entity field (`children`).

Reference: [Spring MVC — Controller arguments and returns](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html)
