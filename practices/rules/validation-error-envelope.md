---
title: Return validation failures as RFC 7807 with a structured errors[] array
impact: HIGH
impactDescription: "Clients can render per-field messages without parsing free-form strings"
tags:
  - validation
  - error
  - rfc-7807
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-004"
verification:
  gradle_task: testPractices
  tag: PRACTICES-VAL-004
upstream:
  - "https://datatracker.ietf.org/doc/html/rfc7807"
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
evidence:
  - upstream_id: rfc-7807
    section: "RFC 7807 — Problem Details + extension members"
    quote: "extension"
  - upstream_id: spring-mvc-validation
    section: "Spring MVC — MethodArgumentNotValidException carries BindingResult"
    quote: "MethodArgumentNotValidException"
  - source_type: external
    citation: "RFC 7807 §3.2 — Extension Members"
    url: "https://datatracker.ietf.org/doc/html/rfc7807#section-3.2"
---

## Return validation failures as RFC 7807 with a structured errors[] array

**Impact: HIGH — Clients can render per-field messages without parsing free-form strings**

A `MethodArgumentNotValidException` carries a `BindingResult` with one entry per violating field. The default Spring response is a generic 400, which forces clients to parse the message string. The contract-friendly response is an RFC 7807 ProblemDetail with the standard `type/title/status/detail` keys *plus* an `errors` extension array of `{field, message}` objects. Clients render per-field error labels next to inputs, dashboards aggregate by field, and the response shape stays uniform across all validation paths.

**Incorrect — generic 400 body forces clients to scrape strings:**

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<String> bad(MethodArgumentNotValidException ex) {
    return ResponseEntity.badRequest()
            .body("validation failed: " + ex.getMessage());
}
```

**Correct — ProblemDetail with an errors[] extension:**

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    pd.setType(URI.create("https://errors.example.com/validation"));
    pd.setTitle("Validation Error");
    pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(e -> Map.of(
                    "field",   e.getField(),
                    "message", e.getDefaultMessage()
            ))
            .toList());
    return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
}
```

Verification: `./gradlew testPractices --tests "*ErrorEnvelope*"` asserts the response carries `application/problem+json`, `type` / `title` / `status` / `detail`, AND an `errors[]` array containing entries for every failing field (`name`, `email`, `username`).

Reference: [RFC 7807 §3.2 Extension Members](https://datatracker.ietf.org/doc/html/rfc7807#section-3.2) · [Spring MVC — Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)
