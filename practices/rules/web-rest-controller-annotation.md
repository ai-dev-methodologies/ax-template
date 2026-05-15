---
title: JSON-API controllers must carry @RestController, never bare @Controller
impact: MEDIUM
impactDescription: "Bare @Controller resolves return values as view names — silent 404 for DTO returns"
tags:
  - web
  - controller
  - spring-mvc
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-WEB-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-WEB-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods.html"
evidence:
  - upstream_id: spring-mvc-controlleradvice
    section: "Spring MVC — @RestController shortcut for @Controller + @ResponseBody"
    quote: "@RestController"
  - source_type: external
    citation: "Spring Framework Reference — Annotated Controllers"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html"
---

## JSON-API controllers must carry @RestController, never bare @Controller

**Impact: MEDIUM — Bare @Controller resolves return values as view names — silent 404 for DTO returns**

`@Controller` is the original Spring MVC annotation; its return values are interpreted as *view names* (the controller is half of a server-rendered MVC pair). A JSON API returning a DTO from a `@Controller` method does NOT serialize the DTO — Spring looks up a view named after the DTO's `toString()` and 404s when none exists. `@RestController` is the meta-annotation `@Controller` + `@ResponseBody`; every method's return value is serialized through the configured converter chain (Jackson → JSON by default). The mechanical remedy is to enforce `@RestController` on every class whose name ends with `Controller`.

**Incorrect — bare @Controller for a JSON endpoint:**

```java
@Controller                                  // resolves return values as view names
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return service.findById(id);         // Spring tries to render a view named after the DTO
    }
}
```

**Correct — @RestController:**

```java
@RestController                              // == @Controller + @ResponseBody
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return service.findById(id);         // serialized as JSON via Jackson
    }
}
```

Verification: `./gradlew testPractices --tests "*RestControllerAnnotation*"` runs an ArchUnit rule that asserts every `*Controller` class under `practices/` is annotated with `@RestController`.

Reference: [Spring Framework — Annotated Controllers](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html)
