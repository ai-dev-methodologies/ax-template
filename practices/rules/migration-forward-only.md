---
title: Migration versions are unique and monotonic — never renumber an applied migration
impact: HIGH
impactDescription: "Renaming an applied migration breaks Flyway's checksum check on every downstream environment"
tags:
  - migration
  - flyway
  - immutability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MIGRATION-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MIGRATION-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/data/sql.html"
evidence:
  - upstream_id: spring-boot-sql-migration
    section: "Spring Boot — Flyway checksum / immutability contract"
    quote: "Flyway"
  - source_type: external
    citation: "Flyway documentation — How Flyway works (immutable history + checksums)"
    url: "https://documentation.red-gate.com/fd/how-flyway-works-271583665.html"
---

## Migration versions are unique and monotonic — never renumber an applied migration

**Impact: HIGH — Renaming an applied migration breaks Flyway's checksum check on every downstream environment**

Once `V003__add_orders_user_id.sql` has been applied in any environment, the migration is recorded in `flyway_schema_history` with a checksum of its file contents. Renaming it to `V002__add_orders_user_id.sql` (because someone "forgot" a step in between) or editing its body to fix a typo changes that checksum. The next `flyway migrate` in that environment detects the checksum mismatch and refuses to start the application. The new edit / renumber lands in a fresh environment fine, breaks every existing one.

The mechanical contract: migration files are immutable after they ship. New schema changes get a new `V{N+1}__...` file. Versions must be unique (no two files with the same `V{N}` prefix) and strictly monotonic.

**Incorrect — renumbering an existing migration:**

```
# Before (shipped):
V001__create_users.sql
V002__create_orders.sql

# After (broken — V002 renamed to V003, new file inserted as V002):
V001__create_users.sql
V002__create_orders_priority_column.sql    ← was a new file
V003__create_orders.sql                    ← was V002 — every existing env's checksum breaks
```

**Correct — forward-only, monotonic:**

```
V001__create_users.sql
V002__create_orders.sql
V003__add_orders_priority_column.sql       ← new file at next version
```

Verification: `./gradlew testPractices --tests "*ForwardOnly*"` parses the leading `V{N}` prefix off each filename and asserts no duplicates + sorted list increases strictly.

Reference: [Flyway — How Flyway works](https://documentation.red-gate.com/fd/how-flyway-works-271583665.html)
