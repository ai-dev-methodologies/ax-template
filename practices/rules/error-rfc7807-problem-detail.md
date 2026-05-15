---
title: Error bodies must follow RFC 7807 application/problem+json
impact: HIGH
impactDescription: "IETF-standardised error envelope — clients can parse problem.type / title / status / detail uniformly"
tags:
  - error
  - rfc-7807
  - api-contract
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ERR-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ERR-002
upstream:
  - "https://datatracker.ietf.org/doc/html/rfc7807"
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html"
evidence:
  - upstream_id: rfc-7807
    section: "RFC 7807 §3.1 — Members of a Problem Details Object"
    quote: "application/problem+json"
  - upstream_id: spring-mvc-exception-handler
    section: "Spring's ProblemDetail support in @ExceptionHandler"
    quote: "ProblemDetail"
  - source_type: external
    citation: "RFC 7807 — Problem Details for HTTP APIs"
    url: "https://datatracker.ietf.org/doc/html/rfc7807"
---

## Error bodies must follow RFC 7807 application/problem+json

**Impact: HIGH — IETF-standardised error envelope — clients can parse problem.type / title / status / detail uniformly**

Every team invents a different shape for error JSON until someone codifies one. RFC 7807 (Problem Details for HTTP APIs) is the existing standard: a media type `application/problem+json` and a base schema with `type` (URI identifying the error class), `title` (short human label), `status` (HTTP status, matches header), `detail` (human-readable description), and optional `instance`. Spring's `ProblemDetail` returns this shape out of the box. Adopting it means clients have one parser for all HTTP errors, not one per service.

**Incorrect — ad-hoc error envelope:**

```java
return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "bad input", "ts", Instant.now().toString()));
```

**Correct — RFC 7807 ProblemDetail:**

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ProblemDetail> badArg(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setType(URI.create("https://errors.example.com/bad-argument"));
    pd.setTitle("Bad Argument");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
}
```

Verification: `./gradlew testPractices --tests "*Rfc7807ProblemDetail*"` asserts the response carries `Content-Type: application/problem+json` and a body with `type / title / status / detail`.

Reference: [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) · [Spring `@ExceptionHandler` + `ProblemDetail`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)
