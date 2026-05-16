# Portability Dashboard

> Advisory only — never gates merges. Snapshot of how the catalog's most-portable rules
> behave when retargeted at external Spring Boot fixtures.

## Latest run

| Run date | Fixtures | Tests | Pass | Fail | Notes |
|----------|----------|-------|------|------|-------|
| 2026-05-16 (initial) | spring-petclinic, spring-realworld | 12 | 11 | 1 | First measurement after D-stage; one real cycle found in realworld |
| 2026-05-16 (N3) | + spring-modulith-example | 18 | 17 | 1 | Modulith added; all 5 rules PASS on modulith — modular monolith naturally cycle-free |

## Per-rule per-fixture matrix (2026-05-16, post-N3)

| Rule | spring-petclinic | spring-realworld | spring-modulith-example | interpretation |
|------|-----------------|-----------------|-------------------------|----------------|
| arch-no-cyclic-package        | PASS | **FAIL** | PASS | realworld has Slice `application` ↔ Slice `infrastructure` bidirectional dependency — genuine architectural smell, rule unchanged. Modulith's modular monolith design is cycle-free as expected (3rd fixture confirms the rule's universality without ambiguity) |
| arch-layer-boundary (Service depends on Controller) | PASS | PASS | PASS | universal (3-of-3 fixtures) |
| arch-layer-boundary (Repository depends on Service/Controller) | PASS | PASS | PASS | universal (3-of-3 fixtures) |
| lang-records-for-dtos          | PASS (vacuous) | PASS (vacuous) | PASS (vacuous) | naming convention is narrow by design — see N2 decision |
| quality-no-system-streams      | PASS | PASS | PASS | universal (3-of-3 fixtures) |
| lang-no-public-mutable-fields | PASS | PASS | PASS | universal (3-of-3 fixtures) |

## What this measurement teaches

1. **arch-no-cyclic-package is genuinely universal** — it caught a real anti-pattern in
   spring-realworld (`io.spring.application.*QueryService` depends on
   `io.spring.infrastructure.mybatis.readservice.*` and vice versa). The rule was never
   written for realworld; the cycle predates the rule by years. This is the strongest
   possible evidence the rule is not ax-template-specific opinion.
2. **lang-records-for-dtos** passes vacuously on both fixtures because neither uses the
   `*Request` / `*Response` suffix convention. The rule is correct as written but its
   *coverage* on external code is limited.
   - **N2 decision (2026-05-16): keep narrow.** Widening to `Dto` / `Form` / `Payload`
     would introduce false positives on non-DTO classes that happen to end in those
     names (e.g. `WebForm`, `JsonPayload`). False positives damage catalog trust more
     than vacuous PASS damages coverage signal. Re-evaluation trigger: a Spring-blessed
     style guide that endorses a specific DTO naming convention.
3. **Three rules (arch-layer-boundary, no-system-streams, no-public-mutable-fields) pass
   on both fixtures** — strong portability signal. These can be considered "validated"
   beyond ax-template.
4. **No false positives on petclinic** (the more vetted of the two fixtures).

## Re-running

```bash
# Build fixtures (Maven petclinic + Gradle realworld + Maven modulith).
# JDK 21 is auto-detected on macOS via /usr/libexec/java_home -v 21.
# Modulith requires JDK 21 — petclinic & realworld accept JDK 17+.
bash practices/evals/portability/run.sh --full

# Then run portability tests
cd backend && ./gradlew testPortability
```

If a fixture has not been built, the corresponding tests skip via JUnit Assumptions —
they do not fail. Output is in `backend/build/reports/tests/testPortability/`.

## Re-evaluation triggers

- Any rule that *fails on petclinic* (the well-vetted fixture) — re-examine the rule;
  it may be ax-template-specific.
- Any rule that *passes vacuously on both fixtures* — consider broadening its detection
  surface.
- Add a third fixture (e.g. spring-modulith-example, spring-boot-realworld variant) to
  break ties when a rule passes on petclinic but fails on realworld; the new fixture is
  the tiebreaker.
