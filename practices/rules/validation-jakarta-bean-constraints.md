---
title: Annotate DTOs with Jakarta Bean Validation + @Valid on the handler
impact: HIGH
impactDescription: "Standard constraint vocabulary; one mechanism handles every endpoint uniformly"
tags:
  - validation
  - jakarta
  - dto
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-VAL-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
  - "https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/"
evidence:
  - upstream_id: spring-mvc-validation
    section: "Spring MVC @Valid + MethodArgumentNotValidException"
    quote: "@Valid"
  - upstream_id: hibernate-validator
    section: "Hibernate Validator — built-in constraints (@NotBlank, @Email, @Size)"
    quote: "@NotBlank"
  - source_type: external
    citation: "Spring Framework Reference — Validation in Spring MVC"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
---

## Annotate DTOs with Jakarta Bean Validation + @Valid on the handler

**Impact: HIGH — Standard constraint vocabulary; one mechanism handles every endpoint uniformly**

Hand-rolled `if (req.name == null || req.name.isBlank()) throw new IllegalArgumentException(...)` scatters validation across every controller method. Jakarta Bean Validation moves the rules onto the DTO, lets Spring run them automatically when `@Valid` is on the handler parameter, and turns the failure into a single typed exception (`MethodArgumentNotValidException`) that one `@ExceptionHandler` can map to a uniform response shape.

**Incorrect — imperative checks scattered across handlers:**

```java
@PostMapping("/users")
public User create(@RequestBody UserCreateRequest req) {
    if (req.name() == null || req.name().isBlank()) {
        throw new IllegalArgumentException("name is required");
    }
    if (req.email() == null || !req.email().contains("@")) {
        throw new IllegalArgumentException("email is invalid");
    }
    return service.create(req);
}
```

**Correct — constraints on the DTO + @Valid on the handler:**

```java
public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 50) String name,
        @NotBlank @Email String email
) {}

@PostMapping("/users")
public User create(@Valid @RequestBody UserCreateRequest req) {
    return service.create(req);
}
```

Verification: `./gradlew testPractices --tests "*JakartaBeanConstraints*"` exercises blank, oversized, and invalid-email payloads, asserts each returns 400, and asserts a valid payload returns 200.

Reference: [Spring MVC — Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html) · [Hibernate Validator Reference](https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/)
