---
title: Translate exceptions through a centralised @RestControllerAdvice
impact: HIGH
impactDescription: "One audited exception → HTTP mapping; per-controller try/catch sprawl is anti-pattern"
tags:
  - error
  - advice
  - spring-mvc
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ERR-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ERR-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html"
evidence:
  - upstream_id: spring-mvc-controlleradvice
    section: "@ControllerAdvice / @RestControllerAdvice"
    quote: "@ControllerAdvice"
  - source_type: external
    citation: "Spring Framework Reference §Controller Advice"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html"
---

## Translate exceptions through a centralised @RestControllerAdvice

**Impact: HIGH — One audited exception → HTTP mapping; per-controller try/catch sprawl is anti-pattern**

When each controller carries its own `try { ... } catch (DomainException e) { return ResponseEntity.status(...).body(...); }`, the same exception ends up mapped differently in different endpoints — sometimes 400, sometimes 422, sometimes 500. The auditable mapping lives in a single `@RestControllerAdvice` class scoped (by `basePackages` or `assignableTypes`) to the relevant slice of the application. Adding a new exception means one new `@ExceptionHandler` method, not a sweep across every controller.

**Incorrect — controller swallows the exception and shapes its own response:**

```java
@GetMapping("/users/{id}")
public ResponseEntity<?> get(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(service.findById(id));
    } catch (NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
```

**Correct — controller is thin; advice owns the mapping:**

```java
@RestControllerAdvice(basePackages = "com.example.users")
public class UsersExceptionAdvice {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
```

Verification: `./gradlew testPractices --tests "*ErrorControllerAdvice*"` hits two demo endpoints and asserts the advice maps `IllegalArgumentException → 400` and `NoSuchElementException → 404`.

Reference: [Spring Framework Reference — Controller Advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
