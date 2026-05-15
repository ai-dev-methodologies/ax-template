---
title: management.endpoints.web.exposure.include must be an explicit allow-list
impact: HIGH
impactDescription: "First wildcard added for debugging ships env / beans / heapdump to production"
tags:
  - actuator
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ACTUATOR-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ACTUATOR-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"
evidence:
  - upstream_id: spring-boot-actuator-endpoints
    section: "Spring Boot Actuator — Endpoint exposure"
    quote: "exposure"
  - source_type: external
    citation: "Spring Boot Reference — Exposing Endpoints"
    url: "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.exposing"
---

## management.endpoints.web.exposure.include must be an explicit allow-list

**Impact: HIGH — First wildcard added for debugging ships env / beans / heapdump to production**

Spring Boot's default web exposure is conservative — `health` and `info` only. Many teams override that default with `management.endpoints.web.exposure.include: '*'` for a debugging session and forget to revert. `*` exposes `env` (full configuration including secret values once `show-values: always` is set), `beans` (the entire DI graph), `heapdump` (memory image), `threaddump` (call stacks), `loggers` (runtime level changes), `metrics`, and — if `spring.application.admin.enabled` — `shutdown`. The mechanical remedy is to require an explicit allow-list and reject the wildcard.

**Incorrect — wildcard exposure:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"                  # exposes env, beans, heapdump, threaddump, loggers...
```

**Correct — explicit allow-list:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,mappings
```

Verification: `./gradlew testPractices --tests "*RestrictExposure*"` reads `application.yml`, finds the `include:` line, rejects `*` wildcards, and rejects any of `env`, `beans`, `heapdump`, `threaddump`, `loggers`, `configprops`, `metrics`, `shutdown`.

Reference: [Spring Boot — Exposing Endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.exposing)
