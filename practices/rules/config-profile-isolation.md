---
title: Move profile-specific config out of application.yml into application-{profile}.yml
impact: MEDIUM
impactDescription: "Profile-gated blocks in one big yaml file are unauditable per environment"
tags:
  - config
  - profiles
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CONFIG-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CONFIG-003
upstream:
  - "https://docs.spring.io/spring-boot/reference/features/profiles.html"
evidence:
  - upstream_id: spring-boot-profiles
    section: "Spring Boot — Profile-specific Files"
    quote: "spring.profiles"
  - source_type: external
    citation: "Spring Boot Reference — Profile-specific Configuration Files"
    url: "https://docs.spring.io/spring-boot/reference/features/profiles.html"
---

## Move profile-specific config out of application.yml into application-{profile}.yml

**Impact: MEDIUM — Profile-gated blocks in one big yaml file are unauditable per environment**

Spring Boot supports two profile-config strategies: separate `application-{profile}.yml` files (one per environment), or inline gated documents inside `application.yml` using `spring.config.activate.on-profile`. The inline form encourages cramming every environment's keys into the base file behind conditionals — and after a few rotations no human can tell which keys actually apply where. Worse, a stale or mis-typed condition silently leaks dev / staging behaviour into prod. The mechanical remedy is to put each environment's keys in its own file (`application-prod.yml`, `application-dev.yml`, `application-test.yml`) and keep the base `application.yml` profile-agnostic.

**Incorrect — environment-specific blocks gated inline:**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:test

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:postgresql://prod-db:5432/app
```

**Correct — separate file per profile:**

```yaml
# application.yml — profile-agnostic
spring:
  application:
    name: my-app

# application-prod.yml — only loaded when prod profile is active
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/app
```

Verification: `./gradlew testPractices --tests "*ProfileIsolation*"` reads `application.yml` and asserts the body does not contain `spring.config.activate.on-profile` or the legacy `on-profile:` key.

Reference: [Spring Boot — Profile-specific Configuration Files](https://docs.spring.io/spring-boot/reference/features/profiles.html#features.profiles.specific)
