---
title: Configure hibernate.jdbc.batch_size + order_inserts for bulk persists
impact: HIGH
impactDescription: "Without batch_size, every persist is a round-trip — 10× or worse latency on bulk paths"
tags:
  - persistence
  - jpa
  - performance
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-003
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
evidence:
  - upstream_id: spring-jpa-fetching
    section: "Spring Data JPA — query methods & JPA properties"
    quote: "hibernate"
  - source_type: external
    citation: "Hibernate User Guide — Batching"
    url: "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#batch"
---

## Configure hibernate.jdbc.batch_size + order_inserts for bulk persists

**Impact: HIGH — Without batch_size, every persist is a round-trip — 10× or worse latency on bulk paths**

The defaults persist one entity per JDBC round-trip. For 200 inserts in one transaction that's 200 round-trips. `hibernate.jdbc.batch_size = N` instructs Hibernate to pack up to N inserts into a single JDBC batch; `hibernate.order_inserts = true` reorders them so same-table inserts cluster (the batch can only span identical statements). Without `order_inserts` the batch is fragmented and most rounds-trips remain. Both flags belong on the EntityManagerFactory; they are not per-method choices.

**Incorrect — defaults: one round-trip per insert:**

```yaml
spring:
  jpa:
    properties:
      # nothing here — silent N round-trips on bulk paths
```

**Correct — batch_size and order_inserts together:**

```yaml
spring:
  jpa:
    properties:
      hibernate.jdbc.batch_size: 20
      hibernate.order_inserts: true
      hibernate.order_updates: true
```

Verification: `./gradlew testPractices --tests "*BatchInsert*"` sets the properties via @TestPropertySource and asserts `EntityManagerFactory.getProperties()` carries them.

Reference: [Hibernate User Guide — Batching](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#batch)
