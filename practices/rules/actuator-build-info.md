---
title: Enable Spring Boot buildInfo() and surface it via /actuator/info
impact: MEDIUM
impactDescription: "Operators need a machine-readable answer to 'what version is running'"
tags:
  - actuator
  - build
  - observability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ACTUATOR-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ACTUATOR-003
upstream:
  - "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"
evidence:
  - upstream_id: spring-boot-actuator-endpoints
    section: "Spring Boot Actuator — /info endpoint and build properties"
    quote: "info"
  - source_type: external
    citation: "Spring Boot Reference — Info Endpoint (build properties)"
    url: "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.info"
---

## Enable Spring Boot buildInfo() and surface it via /actuator/info

**Impact: MEDIUM — Operators need a machine-readable answer to "what version is running"**

When the on-call asks "what's running in prod right now", "git tag" is not enough — multiple commits can ship the same version, and the pod may be on an out-of-date image. The Spring Boot Gradle plugin's `buildInfo()` task generates `META-INF/build-info.properties` at build time (version + groupId + artifactId + name + build time + Git SHA if the Git plugin is also present). The actuator `/info` endpoint then surfaces these fields. The combination becomes a curl-able answer that an alert runbook can hyperlink to.

**Incorrect — no buildInfo, `/actuator/info` returns `{}`:**

```kotlin
plugins {
    id("org.springframework.boot") version "3.2.12"
    id("io.spring.dependency-management") version "1.1.6"
}
// no springBoot { buildInfo() } — /actuator/info is empty
```

**Correct — buildInfo + actuator info enabled:**

```kotlin
plugins {
    id("org.springframework.boot") version "3.2.12"
    id("io.spring.dependency-management") version "1.1.6"
}

springBoot {
    buildInfo()                      // generates META-INF/build-info.properties at build
}
```

```yaml
management:
  info:
    build:
      enabled: true                  # surface build-info.properties on /actuator/info
```

Verification: `./gradlew testPractices --tests "*BuildInfo*"` reads `build.gradle.kts` to assert `springBoot { buildInfo() }` is present and reads `application.yml` to assert `management.info.build.enabled: true`.

Reference: [Spring Boot — Info Endpoint](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.info)
