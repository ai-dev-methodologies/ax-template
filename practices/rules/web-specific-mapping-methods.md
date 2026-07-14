---
title: Use @GetMapping / @PostMapping shortcuts, never bare @RequestMapping
impact: MEDIUM
impactDescription: "@RequestMapping(method=...) is verbose AND footgun — forgetting method= exposes every verb"
tags:
  - web
  - mapping
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-WEB-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-WEB-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
evidence:
  - upstream_id: spring-mvc-requestmapping
    section: "HTTP method-specific shortcut variants of @RequestMapping"
    quote: "HTTP method specific shortcut variants of @RequestMapping: @GetMapping @PostMapping @PutMapping @DeleteMapping @PatchMapping"
  - source_type: external
    citation: "Spring Framework Reference — HTTP method-specific shortcuts"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
---

## Use @GetMapping / @PostMapping shortcuts, never bare @RequestMapping

**Impact: MEDIUM — @RequestMapping(method=...) is verbose AND footgun — forgetting method= exposes every verb**

`@RequestMapping("/users/{id}")` without an explicit `method = RequestMethod.GET` matches every HTTP verb — GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD. A handler that reads a user by id becomes silently reachable by POST, by DELETE, by every verb a curious client tries. The method-specific shortcuts (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`) make the verb mandatory by construction — the annotation name *is* the verb. Class-level `@RequestMapping` for the path prefix is fine; method-level `@RequestMapping` is the anti-pattern.

**Incorrect — bare @RequestMapping on a method:**

```java
@RestController
public class UserController {
    @RequestMapping("/users/{id}")             // matches GET, POST, PUT, DELETE, PATCH, ...
    public UserResponse get(@PathVariable Long id) { ... }
}
```

**Correct — method-specific shortcut:**

```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")                 // GET only
    public UserResponse get(@PathVariable Long id) { ... }
}
```

Verification: `./gradlew testPractices --tests "*SpecificMappingMethods*"` walks every declared method on `PracticesDemoController`, flags any that carry `@RequestMapping` without one of the method-specific shortcuts.

Reference: [Spring MVC — HTTP method-specific shortcuts](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)
