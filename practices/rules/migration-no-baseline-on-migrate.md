---
title: spring.flyway.baseline-on-migrate must not be enabled in base config
impact: HIGH
impactDescription: "Silent baseline on a missing history table makes every prior migration unaudited"
tags:
  - migration
  - flyway
  - production-safety
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MIGRATION-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MIGRATION-003
upstream:
  - "https://docs.spring.io/spring-boot/reference/data/sql.html"
evidence:
  - upstream_id: spring-boot-sql-migration
    section: "Spring Boot — Flyway baseline / repair"
    quote: "Flyway"
  - source_type: external
    citation: "Flyway documentation — Baseline / baseline-on-migrate"
    url: "https://documentation.red-gate.com/fd/baseline-184549760.html"
---

## spring.flyway.baseline-on-migrate must not be enabled in base config

**Impact: HIGH — Silent baseline on a missing history table makes every prior migration unaudited**

`baseline-on-migrate: true` tells Flyway: "if the `flyway_schema_history` table is missing, silently mark the current schema as the baseline (version = `baseline-version`) and proceed". That is exactly what you want exactly once — initial adoption of Flyway on a database that already has tables. It is exactly what you do NOT want in steady-state: a dropped / mis-restored history table re-baselines the environment, every prior migration is "considered applied" without verification, and the next migration runs against undocumented state.

The contract: leave `baseline-on-migrate` unset (Flyway default = false) in the base `application.yml`. If a one-off baseline is needed, do it on a profile-scoped config (`application-baseline.yml`) that is only activated for the one-time operation.

**Incorrect — baseline-on-migrate true by default:**

```yaml
spring:
  flyway:
    baseline-on-migrate: true          # silent footgun: any missing history table re-baselines
```

**Correct — unset / false; profile-scoped if needed at all:**

```yaml
# application.yml (steady-state)
spring:
  flyway:
    enabled: true                      # baseline-on-migrate unset (= false default)

# application-baseline.yml (activated ONLY for the one-time baseline run)
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0
```

Verification: `./gradlew testPractices --tests "*NoBaselineOnMigrate*"` scans the base `application.yml` and asserts the literals `baseline-on-migrate: true` and `baselineOnMigrate: true` do not appear.

Reference: [Flyway — Baseline](https://documentation.red-gate.com/fd/baseline-184549760.html)
