---
title: Controllers must declare produces = application/json explicitly
impact: MEDIUM
impactDescription: "Without explicit produces, content negotiation can serve XML or text depending on Accept header"
tags:
  - web
  - content-negotiation
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-WEB-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-WEB-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
evidence:
  - upstream_id: spring-mvc-controlleradvice
    section: "Spring MVC — @RequestMapping consumes / produces"
    quote: "produces"
  - source_type: external
    citation: "Spring Framework Reference — Producible Media Types"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html#mvc-ann-requestmapping-produces"
---

## Controllers must declare produces = application/json explicitly

**Impact: MEDIUM — Without explicit produces, content negotiation can serve XML or text depending on Accept header**

Spring's default `ContentNegotiationManager` derives the response content type from the request's `Accept` header. If a client sends `Accept: application/xml` and an XML message converter is on the classpath (e.g. via `spring-boot-starter-data-rest`), the same handler suddenly returns XML — a contract shift the API never agreed to. The mechanical remedy is to declare `produces = MediaType.APPLICATION_JSON_VALUE` on the controller's class-level `@RequestMapping`. Spring honors it as a hard requirement: any incompatible Accept header returns 406 instead of silently re-serializing.

**Incorrect — implicit produces, controlled by Accept header:**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) { ... }
}
// Accept: application/xml + Jackson-XML on classpath → XML response (silent contract drift)
```

**Correct — explicit produces declared on the class:**

```java
@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) { ... }
}
// Accept: application/xml → 406 Not Acceptable. JSON contract preserved.
```

Verification: `./gradlew testPractices --tests "*ProducesContract*"` uses reflection on the class-level `@RequestMapping` and asserts `produces()` is non-empty and contains `application/json`.

Reference: [Spring MVC — Producible Media Types](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html#mvc-ann-requestmapping-produces)
