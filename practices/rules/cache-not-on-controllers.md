---
title: "@Cacheable / @CachePut / @CacheEvict are forbidden on @RestController classes"
impact: HIGH
impactDescription: "Controller-layer caching captures request-derived state (principal, locale, headers) — cross-user response leakage"
tags:
  - cache
  - controller
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CACHE-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/cache.html"
evidence:
  - upstream_id: spring-cache-abstraction
    section: "The @Cacheable Annotation — method demarcation"
    quote: "you can use @Cacheable to demarcate methods that are cacheable"
  - source_type: external
    citation: "Spring Framework Reference — Cache Abstraction"
    url: "https://docs.spring.io/spring-framework/reference/integration/cache.html"
---

## @Cacheable / @CachePut / @CacheEvict are forbidden on @RestController classes

**Impact: HIGH — Controller-layer caching captures request-derived state (principal, locale, headers) — cross-user response leakage**

Caching at the controller layer caches the *entire HTTP response* — but the response was built from implicit request context: the authenticated principal, the locale, headers like `Accept-Language`, and any cookies Spring forwards into the model. Two different users hitting the same path with the same path-variables produce two different responses; caching one means the *other* user can be served a response they were never authorized to see. The mechanical remedy is to forbid cache annotations at the controller layer entirely and push caching down to the service layer, where the inputs are explicit method arguments under your control.

**Incorrect — controller-layer caching:**

```java
@RestController
public class UserController {
    @Cacheable("user.profile")                       // caches response — principal-derived state leaks
    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return service.profileFor(auth.getName());
    }
}
```

**Correct — caching at the service layer, controller stays uncached:**

```java
@RestController
public class UserController {
    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return service.profileFor(auth.getName());   // service.profileFor() may cache safely
    }
}

@Service
public class UserService {
    @Cacheable(value = "user.profile", key = "#username", sync = true)
    public UserResponse profileFor(String username) { ... }
}
```

Verification: `./gradlew testPractices --tests "*NotOnControllers*"` runs an ArchUnit rule that asserts no `@RestController` class is annotated with `@Cacheable`, `@CachePut`, or `@CacheEvict`.

Reference: [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
