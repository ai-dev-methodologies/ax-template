---
title: SQL migrations must follow Flyway V{version}__{description}.sql naming
impact: HIGH
impactDescription: "Misnamed migrations are silently skipped by Flyway — schema drift across environments"
tags:
  - migration
  - flyway
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MIGRATION-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MIGRATION-001
upstream:
  - "https://docs.spring.io/spring-boot/reference/data/sql.html"
evidence:
  - upstream_id: spring-boot-sql-migration
    section: "Spring Boot — Flyway versioned migration naming"
    quote: "Typically, migrations are scripts in the form V __ .sql (with an underscore-separated version, such as '1' or '2_1')."
  - source_type: external
    citation: "Flyway documentation — Migration naming convention"
    url: "https://documentation.red-gate.com/fd/migrations-271583622.html"
---

## SQL migrations must follow Flyway V{version}__{description}.sql naming

**Impact: HIGH — Misnamed migrations are silently skipped by Flyway — schema drift across environments**

Flyway picks up migrations by filename. `V001__create_orders.sql` runs at version 1. `001_create_orders.sql` (no `V`, single underscore) does not match the pattern — Flyway silently ignores it, the schema change never happens in any environment that runs `flyway migrate`, and the bug surfaces only when a developer notices the table does not exist. The naming convention is mechanical and enforceable: `V` prefix, version digits (optionally dotted), DOUBLE underscore, description, `.sql`.

**Incorrect — wrong naming, silently skipped:**

```
src/main/resources/db/migration/
├── 001_create_orders.sql            ← no V prefix, single underscore — SKIPPED
├── V1_create_users.sql              ← single underscore between version and description — SKIPPED
└── v002__add_index.sql              ← lowercase v — SKIPPED
```

**Correct — every file matches V{version}__{description}.sql:**

```
src/main/resources/db/migration/
├── V001__create_users.sql
├── V002__create_orders.sql
└── V003__add_orders_user_id_index.sql
```

Verification: `./gradlew testPractices --tests "*VersionedNaming*"` lists `db/migration/*.sql` and asserts each filename matches `^V[0-9]+(?:[._][0-9]+)*__[A-Za-z0-9_]+\.sql$`.

Reference: [Flyway — Migration naming](https://documentation.red-gate.com/fd/migrations-271583622.html) · [Spring Boot — Flyway integration](https://docs.spring.io/spring-boot/reference/data/sql.html)
